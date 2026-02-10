import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/rfid_service.dart';
import '../theme/app_theme.dart';

class ConnectionCard extends StatelessWidget {
  const ConnectionCard({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<RfidService>(
      builder: (context, rfid, _) {
        final isConnected = rfid.isConnected;
        final isConnecting = rfid.connectionState == RfidConnectionState.connecting;

        return Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: _getStatusColor(rfid.connectionState).withOpacity(0.15),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    _getStatusIcon(rfid.connectionState),
                    color: _getStatusColor(rfid.connectionState),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _getStatusText(rfid.connectionState),
                        style: const TextStyle(
                          fontWeight: FontWeight.w600,
                          fontSize: 16,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        rfid.serialPort,
                        style: TextStyle(
                          color: Colors.grey[400],
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ),
                ),
                if (isConnecting)
                  const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                else
                  ElevatedButton(
                    onPressed: isConnected ? rfid.disconnect : rfid.connect,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: isConnected ? AppTheme.errorColor : AppTheme.accentColor,
                      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                    ),
                    child: Text(isConnected ? 'Bontás' : 'Csatlakozás'),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }

  Color _getStatusColor(RfidConnectionState state) {
    switch (state) {
      case RfidConnectionState.connected:
        return AppTheme.accentColor;
      case RfidConnectionState.connecting:
        return AppTheme.warningColor;
      case RfidConnectionState.error:
        return AppTheme.errorColor;
      case RfidConnectionState.disconnected:
        return Colors.grey;
    }
  }

  IconData _getStatusIcon(RfidConnectionState state) {
    switch (state) {
      case RfidConnectionState.connected:
        return Icons.link;
      case RfidConnectionState.connecting:
        return Icons.sync;
      case RfidConnectionState.error:
        return Icons.link_off;
      case RfidConnectionState.disconnected:
        return Icons.link_off;
    }
  }

  String _getStatusText(RfidConnectionState state) {
    switch (state) {
      case RfidConnectionState.connected:
        return 'Csatlakozva';
      case RfidConnectionState.connecting:
        return 'Csatlakozás...';
      case RfidConnectionState.error:
        return 'Hiba';
      case RfidConnectionState.disconnected:
        return 'Nincs kapcsolat';
    }
  }
}
