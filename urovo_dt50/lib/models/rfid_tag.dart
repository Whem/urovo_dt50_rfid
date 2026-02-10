import 'dart:convert';

class RfidTag {
  final String epc;
  final String? tid;
  final int rssi;
  final int readCount;
  final DateTime firstRead;
  DateTime lastRead;

  RfidTag({
    required this.epc,
    this.tid,
    required this.rssi,
    this.readCount = 1,
    DateTime? firstRead,
    DateTime? lastRead,
  })  : firstRead = firstRead ?? DateTime.now(),
        lastRead = lastRead ?? DateTime.now();

  RfidTag copyWith({
    String? epc,
    String? tid,
    int? rssi,
    int? readCount,
    DateTime? firstRead,
    DateTime? lastRead,
  }) {
    return RfidTag(
      epc: epc ?? this.epc,
      tid: tid ?? this.tid,
      rssi: rssi ?? this.rssi,
      readCount: readCount ?? this.readCount,
      firstRead: firstRead ?? this.firstRead,
      lastRead: lastRead ?? this.lastRead,
    );
  }

  String get epcFormatted {
    final buffer = StringBuffer();
    for (int i = 0; i < epc.length; i += 4) {
      if (i > 0) buffer.write(' ');
      buffer.write(epc.substring(i, (i + 4).clamp(0, epc.length)));
    }
    return buffer.toString();
  }

  String get rssiFormatted => '$rssi dBm';

  String? get decodedText {
    final hex = epc.replaceAll(' ', '').trim();
    if (hex.isEmpty || hex.length.isOdd) return null;
    if (!RegExp(r'^[0-9A-Fa-f]+$').hasMatch(hex)) return null;

    final bytes = <int>[];
    for (var i = 0; i < hex.length; i += 2) {
      final part = hex.substring(i, i + 2);
      bytes.add(int.parse(part, radix: 16));
    }

    while (bytes.isNotEmpty && bytes.last == 0) {
      bytes.removeLast();
    }
    if (bytes.isEmpty) return null;

    final text = utf8.decode(bytes, allowMalformed: true).trim();
    if (text.isEmpty) return null;
    final printable = text.runes.where((c) => c >= 32 && c <= 126).length;
    if (printable < 3) return null;
    return text;
  }

  int get signalStrength {
    if (rssi >= -40) return 4;
    if (rssi >= -55) return 3;
    if (rssi >= -70) return 2;
    if (rssi >= -85) return 1;
    return 0;
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is RfidTag && runtimeType == other.runtimeType && epc == other.epc;

  @override
  int get hashCode => epc.hashCode;
}
