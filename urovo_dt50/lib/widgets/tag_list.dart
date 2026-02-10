import 'package:flutter/material.dart';
import '../models/rfid_tag.dart';
import '../theme/app_theme.dart';

class TagList extends StatelessWidget {
  final List<RfidTag> tags;

  const TagList({super.key, required this.tags});

  @override
  Widget build(BuildContext context) {
    if (tags.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.nfc, size: 64, color: Colors.grey[700]),
            const SizedBox(height: 16),
            Text(
              'Nincs beolvasott címke',
              style: TextStyle(color: Colors.grey[500], fontSize: 16),
            ),
            const SizedBox(height: 8),
            Text(
              'Nyomd meg a Scan gombot a kereséshez',
              style: TextStyle(color: Colors.grey[600], fontSize: 13),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      itemCount: tags.length,
      itemBuilder: (context, index) {
        final tag = tags[index];
        return _TagCard(tag: tag, index: index + 1);
      },
    );
  }
}

class _TagCard extends StatelessWidget {
  final RfidTag tag;
  final int index;

  const _TagCard({required this.tag, required this.index});

  @override
  Widget build(BuildContext context) {
    final decoded = tag.decodedText;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: AppTheme.primaryColor.withOpacity(0.15),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Center(
                child: Text(
                  '$index',
                  style: const TextStyle(
                    color: AppTheme.primaryColor,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (decoded != null) ...[
                    Text(
                      decoded,
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 0.2,
                      ),
                    ),
                    const SizedBox(height: 4),
                  ],
                  Text(
                    tag.epcFormatted,
                    style: const TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                      letterSpacing: 0.5,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      _InfoChip(
                        icon: Icons.repeat,
                        value: '${tag.readCount}',
                        color: AppTheme.secondaryColor,
                      ),
                      const SizedBox(width: 8),
                      _InfoChip(
                        icon: Icons.signal_cellular_alt,
                        value: tag.rssiFormatted,
                        color: _getRssiColor(tag.rssi),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            _SignalIndicator(strength: tag.signalStrength),
          ],
        ),
      ),
    );
  }

  Color _getRssiColor(int rssi) {
    if (rssi >= -50) return AppTheme.accentColor;
    if (rssi >= -65) return AppTheme.warningColor;
    return AppTheme.errorColor;
  }
}

class _InfoChip extends StatelessWidget {
  final IconData icon;
  final String value;
  final Color color;

  const _InfoChip({
    required this.icon,
    required this.value,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 12, color: color),
          const SizedBox(width: 4),
          Text(
            value,
            style: TextStyle(fontSize: 11, color: color, fontWeight: FontWeight.w500),
          ),
        ],
      ),
    );
  }
}

class _SignalIndicator extends StatelessWidget {
  final int strength;

  const _SignalIndicator({required this.strength});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.end,
      children: List.generate(4, (i) {
        final isActive = i < strength;
        return Container(
          width: 4,
          height: 8 + (i * 4).toDouble(),
          margin: const EdgeInsets.only(left: 2),
          decoration: BoxDecoration(
            color: isActive ? _getColor(strength) : Colors.grey[700],
            borderRadius: BorderRadius.circular(2),
          ),
        );
      }),
    );
  }

  Color _getColor(int strength) {
    if (strength >= 3) return AppTheme.accentColor;
    if (strength >= 2) return AppTheme.warningColor;
    return AppTheme.errorColor;
  }
}
