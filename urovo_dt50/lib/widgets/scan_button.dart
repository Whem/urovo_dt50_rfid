import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/rfid_service.dart';
import '../theme/app_theme.dart';

class ScanButton extends StatelessWidget {
  const ScanButton({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<RfidService>(
      builder: (context, rfid, _) {
        final isScanning = rfid.isScanning;
        final isConnected = rfid.isConnected;

        return GestureDetector(
          onTap: isConnected && !isScanning ? () => rfid.singleScan() : null,
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 200),
            height: 64,
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: isConnected
                    ? (isScanning
                        ? [AppTheme.errorColor, AppTheme.errorColor.withRed(200)]
                        : [AppTheme.primaryColor, AppTheme.secondaryColor])
                    : [Colors.grey[700]!, Colors.grey[600]!],
              ),
              borderRadius: BorderRadius.circular(16),
              boxShadow: isScanning
                  ? [
                      BoxShadow(
                        color: AppTheme.primaryColor.withOpacity(0.4),
                        blurRadius: 16,
                        spreadRadius: 2,
                      ),
                    ]
                  : null,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (isScanning)
                  const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                    ),
                  )
                else
                  Icon(
                    isConnected ? Icons.sensors : Icons.sensors_off,
                    color: Colors.white,
                    size: 28,
                  ),
                const SizedBox(width: 12),
                Text(
                  isScanning ? 'Keresés...' : (isConnected ? 'SCAN' : 'Nincs kapcsolat'),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 1,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
