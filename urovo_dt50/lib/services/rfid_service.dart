import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import '../models/rfid_tag.dart';

enum RfidConnectionState { disconnected, connecting, connected, error }
enum ScanState { idle, scanning }

class RfidService extends ChangeNotifier {
  static const _channel = MethodChannel('com.urovo.dt50/rfid');
  
  RfidConnectionState _connectionState = RfidConnectionState.disconnected;
  ScanState _scanState = ScanState.idle;
  String? _errorMessage;
  final List<RfidTag> _tags = [];
  int _totalReads = 0;
  int _outputPower = 30;
  String _serialPort = '/dev/ttyHSL0';
  int _baudRate = 115200;

  RfidConnectionState get connectionState => _connectionState;
  ScanState get scanState => _scanState;
  String? get errorMessage => _errorMessage;
  List<RfidTag> get tags => List.unmodifiable(_tags);
  int get totalReads => _totalReads;
  int get uniqueTags => _tags.length;
  int get outputPower => _outputPower;
  String get serialPort => _serialPort;
  int get baudRate => _baudRate;
  bool get isConnected => _connectionState == RfidConnectionState.connected;
  bool get isScanning => _scanState == ScanState.scanning;

  RfidService() {
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onTagRead':
        _handleTagRead(call.arguments as Map);
        break;
      case 'onConnectionChanged':
        _handleConnectionChanged(call.arguments as bool);
        break;
      case 'onScanningStateChanged':
        _handleScanningStateChanged(call.arguments as bool);
        break;
      case 'onError':
        _handleError(call.arguments as String);
        break;
    }
  }
  
  void _handleScanningStateChanged(bool scanning) {
    if (scanning && _scanState != ScanState.scanning) {
      // Trigger pressed - clear list at start of new scan session
      _tags.clear();
      _totalReads = 0;
    }
    _scanState = scanning ? ScanState.scanning : ScanState.idle;
    notifyListeners();
  }

  void _handleTagRead(Map data) {
    final epc = data['epc'] as String;
    final rssi = data['rssi'] as int? ?? -70;
    final tid = data['tid'] as String?;

    _totalReads++;

    final existingIndex = _tags.indexWhere((t) => t.epc == epc);
    if (existingIndex >= 0) {
      final existing = _tags.removeAt(existingIndex);
      _tags.insert(0, existing.copyWith(
        rssi: rssi,
        readCount: existing.readCount + 1,
        lastRead: DateTime.now(),
      ));
    } else {
      _tags.insert(0, RfidTag(epc: epc, tid: tid, rssi: rssi));
    }
    notifyListeners();
  }

  void _handleConnectionChanged(bool connected) {
    _connectionState = connected ? RfidConnectionState.connected : RfidConnectionState.disconnected;
    if (!connected) {
      _scanState = ScanState.idle;
    }
    notifyListeners();
  }

  void _handleError(String message) {
    _errorMessage = message;
    _connectionState = RfidConnectionState.error;
    notifyListeners();
  }

  Future<bool> connect() async {
    try {
      _connectionState = RfidConnectionState.connecting;
      _errorMessage = null;
      notifyListeners();

      final result = await _channel.invokeMethod<bool>('connect', {
        'port': _serialPort,
        'baudRate': _baudRate,
      });

      _connectionState = result == true ? RfidConnectionState.connected : RfidConnectionState.error;
      if (result != true) {
        _errorMessage = 'Nem sikerült csatlakozni az RFID modulhoz';
      }
      notifyListeners();
      return result == true;
    } catch (e) {
      _connectionState = RfidConnectionState.error;
      _errorMessage = e.toString();
      notifyListeners();
      return false;
    }
  }

  Future<void> disconnect() async {
    try {
      await _channel.invokeMethod('disconnect');
      _connectionState = RfidConnectionState.disconnected;
      _scanState = ScanState.idle;
      notifyListeners();
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
    }
  }

  Future<bool> startScan({bool clearList = false}) async {
    if (!isConnected) return false;
    try {
      if (clearList) {
        _tags.clear();
        _totalReads = 0;
        notifyListeners();
      }
      
      final result = await _channel.invokeMethod<bool>('startInventory');
      if (result == true) {
        _scanState = ScanState.scanning;
        notifyListeners();
      }
      return result == true;
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
      return false;
    }
  }

  Future<void> singleScan() async {
    if (!isConnected) return;
    _tags.clear();
    _totalReads = 0;
    notifyListeners();
    
    await _channel.invokeMethod<bool>('startInventory');
    _scanState = ScanState.scanning;
    notifyListeners();
    
    await Future.delayed(const Duration(milliseconds: 800));
    await stopScan();
  }

  Future<void> stopScan() async {
    try {
      await _channel.invokeMethod('stopInventory');
      _scanState = ScanState.idle;
      notifyListeners();
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
    }
  }

  Future<bool> writeEpc(String targetEpc, String newEpc, {String password = '00000000'}) async {
    try {
      final result = await _channel.invokeMethod<bool>('writeEpc', {
        'targetEpc': targetEpc,
        'newEpc': newEpc,
        'password': password,
      });
      return result == true;
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
      return false;
    }
  }

  Future<String?> readMemory(String epc, int memBank, int startAddr, int length, {String password = '00000000'}) async {
    try {
      final result = await _channel.invokeMethod<String>('readMemory', {
        'epc': epc,
        'memBank': memBank,
        'startAddr': startAddr,
        'length': length,
        'password': password,
      });
      return result;
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
      return null;
    }
  }

  Future<bool> writeMemory(String epc, int memBank, int startAddr, String data, {int? length, String password = '00000000'}) async {
    try {
      final result = await _channel.invokeMethod<bool>('writeMemory', {
        'epc': epc,
        'memBank': memBank,
        'startAddr': startAddr,
        if (length != null) 'length': length,
        'data': data,
        'password': password,
      });
      return result == true;
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
      return false;
    }
  }

  Future<void> setOutputPower(int power) async {
    try {
      await _channel.invokeMethod('setOutputPower', {'power': power});
      _outputPower = power;
      notifyListeners();
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
    }
  }

  void clearTags() {
    _tags.clear();
    _totalReads = 0;
    notifyListeners();
  }

  void updateSettings({String? port, int? baudRate}) {
    if (port != null) _serialPort = port;
    if (baudRate != null) _baudRate = baudRate;
    notifyListeners();
  }

  String getTagsAsText() {
    final lines = _tags
        .map((tag) => tag.decodedText ?? tag.epc)
        .toList();
    return lines.join('\n');
  }
}
