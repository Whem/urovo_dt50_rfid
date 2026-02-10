import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/rfid_service.dart';
import '../theme/app_theme.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late TextEditingController _portController;
  int _selectedBaudRate = 115200;
  double _power = 26;

  final List<int> _baudRates = [9600, 19200, 38400, 57600, 115200];

  @override
  void initState() {
    super.initState();
    final rfid = context.read<RfidService>();
    _portController = TextEditingController(text: rfid.serialPort);
    _selectedBaudRate = rfid.baudRate;
    _power = rfid.outputPower.toDouble();
  }

  @override
  void dispose() {
    _portController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildSection(
            'Connection',
            Icons.cable,
            [
              _buildTextField(
                'Serial Port',
                _portController,
                Icons.usb,
              ),
              const SizedBox(height: 16),
              _buildDropdown(
                'Baud Rate',
                _selectedBaudRate,
                _baudRates,
                (value) => setState(() => _selectedBaudRate = value!),
              ),
            ],
          ),
          const SizedBox(height: 24),
          _buildSection(
            'RFID Settings',
            Icons.settings_input_antenna,
            [
              _buildPowerSlider(),
            ],
          ),
          const SizedBox(height: 24),
          _buildSection(
            'Application',
            Icons.info_outline,
            [
              _buildInfoTile('Version', '1.0.0'),
              _buildInfoTile('Platform', 'Urovo DT50'),
            ],
          ),
          const SizedBox(height: 32),
          ElevatedButton.icon(
            onPressed: _saveSettings,
            icon: const Icon(Icons.save),
            label: const Text('Save'),
            style: ElevatedButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSection(String title, IconData icon, List<Widget> children) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, color: AppTheme.primaryColor, size: 20),
                const SizedBox(width: 8),
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _buildTextField(String label, TextEditingController controller, IconData icon) {
    return TextField(
      controller: controller,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        filled: true,
        fillColor: AppTheme.darkSurface.withOpacity(0.5),
      ),
    );
  }

  Widget _buildDropdown(String label, int value, List<int> items, ValueChanged<int?> onChanged) {
    return DropdownButtonFormField<int>(
      value: value,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: const Icon(Icons.speed),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        filled: true,
        fillColor: AppTheme.darkSurface.withOpacity(0.5),
      ),
      items: items.map((rate) => DropdownMenuItem(
        value: rate,
        child: Text('$rate bps'),
      )).toList(),
      onChanged: onChanged,
    );
  }

  Widget _buildPowerSlider() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text('Output Power'),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              decoration: BoxDecoration(
                color: AppTheme.primaryColor.withOpacity(0.2),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                '${_power.toInt()} dBm',
                style: const TextStyle(
                  color: AppTheme.primaryColor,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        SliderTheme(
          data: SliderTheme.of(context).copyWith(
            activeTrackColor: AppTheme.primaryColor,
            inactiveTrackColor: AppTheme.darkSurface,
            thumbColor: AppTheme.primaryColor,
            overlayColor: AppTheme.primaryColor.withOpacity(0.2),
          ),
          child: Slider(
            value: _power,
            min: 0,
            max: 33,
            divisions: 33,
            onChanged: (value) => setState(() => _power = value),
          ),
        ),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('0 dBm', style: TextStyle(color: Colors.grey[500], fontSize: 12)),
            Text('33 dBm', style: TextStyle(color: Colors.grey[500], fontSize: 12)),
          ],
        ),
      ],
    );
  }

  Widget _buildInfoTile(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(color: Colors.grey[400])),
          Text(value, style: const TextStyle(fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }

  void _saveSettings() {
    final rfid = context.read<RfidService>();
    rfid.updateSettings(
      port: _portController.text,
      baudRate: _selectedBaudRate,
    );
    rfid.setOutputPower(_power.toInt());
    
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Settings saved'),
        backgroundColor: AppTheme.accentColor,
      ),
    );
    Navigator.pop(context);
  }
}
