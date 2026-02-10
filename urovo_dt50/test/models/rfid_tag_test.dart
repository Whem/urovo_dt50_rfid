import 'package:flutter_test/flutter_test.dart';
import 'package:urovo_dt50/models/rfid_tag.dart';

void main() {
  group('RfidTag', () {
    test('constructor sets default values', () {
      final tag = RfidTag(epc: 'AABB1122', rssi: -55);
      expect(tag.epc, 'AABB1122');
      expect(tag.rssi, -55);
      expect(tag.readCount, 1);
      expect(tag.tid, isNull);
      expect(tag.firstRead, isNotNull);
      expect(tag.lastRead, isNotNull);
    });

    test('copyWith creates modified copy', () {
      final tag = RfidTag(epc: 'AABB1122', rssi: -55);
      final copy = tag.copyWith(rssi: -40, readCount: 5);
      expect(copy.epc, 'AABB1122');
      expect(copy.rssi, -40);
      expect(copy.readCount, 5);
      expect(copy.firstRead, tag.firstRead);
    });

    test('copyWith preserves original when no args', () {
      final tag = RfidTag(epc: 'AABB', rssi: -70, tid: 'TID1');
      final copy = tag.copyWith();
      expect(copy.epc, tag.epc);
      expect(copy.rssi, tag.rssi);
      expect(copy.tid, tag.tid);
      expect(copy.readCount, tag.readCount);
    });

    group('epcFormatted', () {
      test('formats EPC in 4-char groups', () {
        final tag = RfidTag(epc: 'AABBCCDD1122', rssi: -50);
        expect(tag.epcFormatted, 'AABB CCDD 1122');
      });

      test('handles short EPC', () {
        final tag = RfidTag(epc: 'AB', rssi: -50);
        expect(tag.epcFormatted, 'AB');
      });

      test('handles exactly 4 chars', () {
        final tag = RfidTag(epc: 'AABB', rssi: -50);
        expect(tag.epcFormatted, 'AABB');
      });

      test('handles 5 chars (partial last group)', () {
        final tag = RfidTag(epc: 'AABBC', rssi: -50);
        expect(tag.epcFormatted, 'AABB C');
      });

      test('handles empty EPC', () {
        final tag = RfidTag(epc: '', rssi: -50);
        expect(tag.epcFormatted, '');
      });
    });

    test('rssiFormatted returns correct string', () {
      final tag = RfidTag(epc: 'AA', rssi: -65);
      expect(tag.rssiFormatted, '-65 dBm');
    });

    group('decodedText', () {
      test('decodes ASCII hex to text', () {
        // "Hello" = 48 65 6C 6C 6F
        final tag = RfidTag(epc: '48656C6C6F', rssi: -50);
        expect(tag.decodedText, 'Hello');
      });

      test('decodes with trailing zeros stripped', () {
        // "Test" + null bytes = 54 65 73 74 00 00
        final tag = RfidTag(epc: '546573740000', rssi: -50);
        expect(tag.decodedText, 'Test');
      });

      test('returns null for non-hex string', () {
        final tag = RfidTag(epc: 'ZZZZ', rssi: -50);
        expect(tag.decodedText, isNull);
      });

      test('returns null for empty EPC', () {
        final tag = RfidTag(epc: '', rssi: -50);
        expect(tag.decodedText, isNull);
      });

      test('returns null for odd-length hex', () {
        final tag = RfidTag(epc: 'ABC', rssi: -50);
        expect(tag.decodedText, isNull);
      });

      test('returns null for all-zero bytes', () {
        final tag = RfidTag(epc: '00000000', rssi: -50);
        expect(tag.decodedText, isNull);
      });

      test('returns null when fewer than 3 printable chars', () {
        // 0x01 0x02 = non-printable
        final tag = RfidTag(epc: '0102', rssi: -50);
        expect(tag.decodedText, isNull);
      });

      test('decodes mixed content with enough printable chars', () {
        // "ABC" = 41 42 43
        final tag = RfidTag(epc: '414243', rssi: -50);
        expect(tag.decodedText, 'ABC');
      });
    });

    group('signalStrength', () {
      test('returns 4 for strong signal', () {
        expect(RfidTag(epc: 'AA', rssi: -30).signalStrength, 4);
        expect(RfidTag(epc: 'AA', rssi: -40).signalStrength, 4);
      });

      test('returns 3 for good signal', () {
        expect(RfidTag(epc: 'AA', rssi: -41).signalStrength, 3);
        expect(RfidTag(epc: 'AA', rssi: -55).signalStrength, 3);
      });

      test('returns 2 for moderate signal', () {
        expect(RfidTag(epc: 'AA', rssi: -56).signalStrength, 2);
        expect(RfidTag(epc: 'AA', rssi: -70).signalStrength, 2);
      });

      test('returns 1 for weak signal', () {
        expect(RfidTag(epc: 'AA', rssi: -71).signalStrength, 1);
        expect(RfidTag(epc: 'AA', rssi: -85).signalStrength, 1);
      });

      test('returns 0 for very weak signal', () {
        expect(RfidTag(epc: 'AA', rssi: -86).signalStrength, 0);
        expect(RfidTag(epc: 'AA', rssi: -100).signalStrength, 0);
      });
    });

    group('equality', () {
      test('tags with same EPC are equal', () {
        final a = RfidTag(epc: 'AABB', rssi: -50);
        final b = RfidTag(epc: 'AABB', rssi: -70);
        expect(a, equals(b));
      });

      test('tags with different EPC are not equal', () {
        final a = RfidTag(epc: 'AABB', rssi: -50);
        final b = RfidTag(epc: 'CCDD', rssi: -50);
        expect(a, isNot(equals(b)));
      });

      test('hashCode is consistent with equality', () {
        final a = RfidTag(epc: 'AABB', rssi: -50);
        final b = RfidTag(epc: 'AABB', rssi: -70);
        expect(a.hashCode, equals(b.hashCode));
      });

      test('can be used in Set for deduplication', () {
        final set = <RfidTag>{};
        set.add(RfidTag(epc: 'AABB', rssi: -50));
        set.add(RfidTag(epc: 'AABB', rssi: -70));
        set.add(RfidTag(epc: 'CCDD', rssi: -50));
        expect(set.length, 2);
      });
    });
  });
}
