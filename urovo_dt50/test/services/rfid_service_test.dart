import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:urovo_dt50/services/rfid_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late RfidService service;
  late List<MethodCall> log;

  setUp(() {
    log = [];
    service = RfidService();

    // Mock the platform channel
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      const MethodChannel('com.urovo.dt50/rfid'),
      (MethodCall methodCall) async {
        log.add(methodCall);
        switch (methodCall.method) {
          case 'connect':
            return true;
          case 'disconnect':
            return true;
          case 'startInventory':
            return true;
          case 'stopInventory':
            return true;
          case 'setOutputPower':
            return true;
          case 'writeEpc':
            return true;
          case 'readMemory':
            return 'AABBCCDD';
          case 'writeMemory':
            return true;
          default:
            return null;
        }
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      const MethodChannel('com.urovo.dt50/rfid'),
      null,
    );
  });

  group('RfidService - initial state', () {
    test('starts disconnected', () {
      expect(service.connectionState, RfidConnectionState.disconnected);
      expect(service.isConnected, false);
    });

    test('starts not scanning', () {
      expect(service.scanState, ScanState.idle);
      expect(service.isScanning, false);
    });

    test('starts with empty tags', () {
      expect(service.tags, isEmpty);
      expect(service.totalReads, 0);
      expect(service.uniqueTags, 0);
    });

    test('has default settings', () {
      expect(service.serialPort, '/dev/ttyHSL0');
      expect(service.baudRate, 115200);
      expect(service.outputPower, 30);
    });

    test('has no error', () {
      expect(service.errorMessage, isNull);
    });
  });

  group('RfidService - updateSettings', () {
    test('updates port', () {
      service.updateSettings(port: '/dev/ttyS1');
      expect(service.serialPort, '/dev/ttyS1');
    });

    test('updates baud rate', () {
      service.updateSettings(baudRate: 9600);
      expect(service.baudRate, 9600);
    });

    test('updates both at once', () {
      service.updateSettings(port: '/dev/ttyS2', baudRate: 57600);
      expect(service.serialPort, '/dev/ttyS2');
      expect(service.baudRate, 57600);
    });

    test('notifies listeners on update', () {
      int notifyCount = 0;
      service.addListener(() => notifyCount++);
      service.updateSettings(port: '/dev/ttyS1');
      expect(notifyCount, 1);
    });
  });

  group('RfidService - clearTags', () {
    test('clears tags and resets count', () {
      // Simulate some tag reads via the callback handler
      service.clearTags();
      expect(service.tags, isEmpty);
      expect(service.totalReads, 0);
    });

    test('notifies listeners', () {
      int notifyCount = 0;
      service.addListener(() => notifyCount++);
      service.clearTags();
      expect(notifyCount, 1);
    });
  });

  group('RfidService - getTagsAsText', () {
    test('returns empty string when no tags', () {
      expect(service.getTagsAsText(), '');
    });
  });

  group('RfidService - connect', () {
    test('sends connect method call', () async {
      await service.connect();
      expect(log.any((c) => c.method == 'connect'), true);
    });

    test('transitions through connecting state', () async {
      final states = <RfidConnectionState>[];
      service.addListener(() => states.add(service.connectionState));
      await service.connect();
      expect(states.first, RfidConnectionState.connecting);
      expect(states.last, RfidConnectionState.connected);
    });

    test('sets isConnected after successful connect', () async {
      await service.connect();
      expect(service.isConnected, true);
    });
  });

  group('RfidService - disconnect', () {
    test('sends disconnect method call', () async {
      await service.connect();
      log.clear();
      await service.disconnect();
      expect(log.any((c) => c.method == 'disconnect'), true);
    });

    test('sets disconnected state', () async {
      await service.connect();
      await service.disconnect();
      expect(service.connectionState, RfidConnectionState.disconnected);
      expect(service.isConnected, false);
    });
  });

  group('RfidService - setOutputPower', () {
    test('sends setOutputPower with correct argument', () async {
      await service.setOutputPower(25);
      expect(log.last.method, 'setOutputPower');
      expect(log.last.arguments['power'], 25);
    });

    test('updates outputPower locally', () async {
      await service.setOutputPower(20);
      expect(service.outputPower, 20);
    });
  });

  group('RfidService - writeEpc', () {
    test('sends writeEpc with correct arguments', () async {
      final result = await service.writeEpc('AABB', 'CCDD', password: '00000000');
      expect(result, true);
      final call = log.firstWhere((c) => c.method == 'writeEpc');
      expect(call.arguments['targetEpc'], 'AABB');
      expect(call.arguments['newEpc'], 'CCDD');
      expect(call.arguments['password'], '00000000');
    });
  });

  group('RfidService - readMemory', () {
    test('sends readMemory and returns result', () async {
      final result = await service.readMemory('AABB', 1, 2, 6);
      expect(result, 'AABBCCDD');
      final call = log.firstWhere((c) => c.method == 'readMemory');
      expect(call.arguments['epc'], 'AABB');
      expect(call.arguments['memBank'], 1);
      expect(call.arguments['startAddr'], 2);
      expect(call.arguments['length'], 6);
    });
  });

  group('RfidService - writeMemory', () {
    test('sends writeMemory with correct arguments', () async {
      final result = await service.writeMemory('AABB', 3, 0, 'DDEE', length: 2);
      expect(result, true);
      final call = log.firstWhere((c) => c.method == 'writeMemory');
      expect(call.arguments['epc'], 'AABB');
      expect(call.arguments['memBank'], 3);
      expect(call.arguments['startAddr'], 0);
      expect(call.arguments['data'], 'DDEE');
      expect(call.arguments['length'], 2);
    });
  });

  group('RfidService - onTagRead callback', () {
    test('adds tag on callback', () async {
      // Simulate native calling onTagRead
      final channel = const MethodChannel('com.urovo.dt50/rfid');
      final codec = channel.codec;
      final data = codec.encodeMethodCall(
        const MethodCall('onTagRead', {'epc': 'AABB1122', 'rssi': -55, 'tid': ''}),
      );
      await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage('com.urovo.dt50/rfid', data, (ByteData? reply) {});

      expect(service.tags.length, 1);
      expect(service.tags.first.epc, 'AABB1122');
      expect(service.tags.first.rssi, -55);
      expect(service.totalReads, 1);
    });

    test('increments readCount for duplicate EPC', () async {
      final channel = const MethodChannel('com.urovo.dt50/rfid');
      final codec = channel.codec;

      for (int i = 0; i < 3; i++) {
        final data = codec.encodeMethodCall(
          const MethodCall('onTagRead', {'epc': 'AABB', 'rssi': -50, 'tid': ''}),
        );
        await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .handlePlatformMessage('com.urovo.dt50/rfid', data, (ByteData? reply) {});
      }

      expect(service.tags.length, 1);
      expect(service.tags.first.readCount, 3);
      expect(service.totalReads, 3);
    });

    test('puts most recent tag at index 0', () async {
      final channel = const MethodChannel('com.urovo.dt50/rfid');
      final codec = channel.codec;

      final data1 = codec.encodeMethodCall(
        const MethodCall('onTagRead', {'epc': 'TAG1', 'rssi': -50, 'tid': ''}),
      );
      await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage('com.urovo.dt50/rfid', data1, (ByteData? reply) {});

      final data2 = codec.encodeMethodCall(
        const MethodCall('onTagRead', {'epc': 'TAG2', 'rssi': -60, 'tid': ''}),
      );
      await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage('com.urovo.dt50/rfid', data2, (ByteData? reply) {});

      expect(service.tags.first.epc, 'TAG2');
      expect(service.tags.length, 2);
    });
  });

  group('RfidService - onConnectionChanged callback', () {
    test('updates connection state on true', () async {
      final channel = const MethodChannel('com.urovo.dt50/rfid');
      final codec = channel.codec;
      final data = codec.encodeMethodCall(
        const MethodCall('onConnectionChanged', true),
      );
      await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage('com.urovo.dt50/rfid', data, (ByteData? reply) {});

      expect(service.connectionState, RfidConnectionState.connected);
    });

    test('updates connection state on false', () async {
      final channel = const MethodChannel('com.urovo.dt50/rfid');
      final codec = channel.codec;
      final data = codec.encodeMethodCall(
        const MethodCall('onConnectionChanged', false),
      );
      await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage('com.urovo.dt50/rfid', data, (ByteData? reply) {});

      expect(service.connectionState, RfidConnectionState.disconnected);
    });
  });

  group('RfidService - onScanningStateChanged callback', () {
    test('clears tags when scanning starts', () async {
      // First add a tag
      final channel = const MethodChannel('com.urovo.dt50/rfid');
      final codec = channel.codec;
      final tagData = codec.encodeMethodCall(
        const MethodCall('onTagRead', {'epc': 'AABB', 'rssi': -50, 'tid': ''}),
      );
      await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage('com.urovo.dt50/rfid', tagData, (ByteData? reply) {});
      expect(service.tags.length, 1);

      // Now start scanning
      final scanData = codec.encodeMethodCall(
        const MethodCall('onScanningStateChanged', true),
      );
      await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage('com.urovo.dt50/rfid', scanData, (ByteData? reply) {});

      expect(service.isScanning, true);
      expect(service.tags, isEmpty);
      expect(service.totalReads, 0);
    });

    test('sets idle when scanning stops', () async {
      final channel = const MethodChannel('com.urovo.dt50/rfid');
      final codec = channel.codec;

      // Start scanning
      final startData = codec.encodeMethodCall(
        const MethodCall('onScanningStateChanged', true),
      );
      await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage('com.urovo.dt50/rfid', startData, (ByteData? reply) {});

      // Stop scanning
      final stopData = codec.encodeMethodCall(
        const MethodCall('onScanningStateChanged', false),
      );
      await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage('com.urovo.dt50/rfid', stopData, (ByteData? reply) {});

      expect(service.isScanning, false);
      expect(service.scanState, ScanState.idle);
    });
  });

  group('RfidService - onError callback', () {
    test('sets error state', () async {
      final channel = const MethodChannel('com.urovo.dt50/rfid');
      final codec = channel.codec;
      final data = codec.encodeMethodCall(
        const MethodCall('onError', 'Something went wrong'),
      );
      await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage('com.urovo.dt50/rfid', data, (ByteData? reply) {});

      expect(service.connectionState, RfidConnectionState.error);
      expect(service.errorMessage, 'Something went wrong');
    });
  });
}
