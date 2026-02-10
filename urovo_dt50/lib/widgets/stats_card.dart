import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/rfid_service.dart';
import '../theme/app_theme.dart';

class StatsCard extends StatelessWidget {
  const StatsCard({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<RfidService>(
      builder: (context, rfid, _) {
        return Row(
          children: [
            Expanded(
              child: _StatItem(
                icon: Icons.nfc,
                value: '${rfid.uniqueTags}',
                label: 'Egyedi',
                color: AppTheme.primaryColor,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _StatItem(
                icon: Icons.repeat,
                value: '${rfid.totalReads}',
                label: 'Olvasás',
                color: AppTheme.secondaryColor,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _StatItem(
                icon: Icons.bolt,
                value: '${rfid.outputPower}',
                label: 'dBm',
                color: AppTheme.warningColor,
              ),
            ),
          ],
        );
      },
    );
  }
}

class _StatItem extends StatelessWidget {
  final IconData icon;
  final String value;
  final String label;
  final Color color;

  const _StatItem({
    required this.icon,
    required this.value,
    required this.label,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 12),
        child: Column(
          children: [
            Icon(icon, color: color, size: 24),
            const SizedBox(height: 8),
            Text(
              value,
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey[400],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
