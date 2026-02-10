import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/rfid_tag.dart';
import '../services/rfid_service.dart';
import '../theme/app_theme.dart';

class WriteScreen extends StatefulWidget {
  const WriteScreen({super.key});

  @override
  State<WriteScreen> createState() => _WriteScreenState();
}

class _WriteScreenState extends State<WriteScreen> {
  final _epcController = TextEditingController();
  final _passwordController = TextEditingController(text: '00000000');
  final _contentController = TextEditingController();
  
  String? _selectedTag;
  int _selectedMemBank = 1;
  int _startAddr = 2;
  int _length = 6;
  bool _isLoading = false;
  String? _result;
  bool _isSuccess = false;

  final List<Map<String, dynamic>> _memBanks = [
    {'id': 0, 'name': 'Reserved', 'icon': Icons.lock},
    {'id': 1, 'name': 'EPC', 'icon': Icons.qr_code},
    {'id': 2, 'name': 'TID', 'icon': Icons.fingerprint},
    {'id': 3, 'name': 'User', 'icon': Icons.person},
  ];

  @override
  void dispose() {
    _epcController.dispose();
    _passwordController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<RfidService>(
      builder: (context, rfid, _) {
        return SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildTagSelector(rfid),
              const SizedBox(height: 12),
              _buildQuickEpcWrite(rfid),
              const SizedBox(height: 16),
              _buildMemoryBankSelector(),
              const SizedBox(height: 16),
              _buildAddressConfig(),
              const SizedBox(height: 16),
              _buildPasswordField(),
              const SizedBox(height: 16),
              _buildContentField(),
              const SizedBox(height: 24),
              _buildActionButtons(rfid),
              if (_result != null) ...[
                const SizedBox(height: 16),
                _buildResultCard(),
              ],
            ],
          ),
        );
      },
    );
  }

  Widget _buildTagSelector(RfidService rfid) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.nfc, color: AppTheme.primaryColor, size: 20),
                const SizedBox(width: 8),
                const Text('Cél címke', style: TextStyle(fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: _selectedTag,
              isExpanded: true,
              hint: const Text('Válassz címkét...'),
              decoration: InputDecoration(
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                filled: true,
                fillColor: AppTheme.darkSurface.withOpacity(0.5),
                contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              ),
              items: rfid.tags.map((tag) => DropdownMenuItem(
                value: tag.epc,
                child: Text(
                  '${tag.rssi}dB ${tag.epc.length > 16 ? '${tag.epc.substring(0, 16)}...' : tag.epc}',
                  style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
                  overflow: TextOverflow.ellipsis,
                ),
              )).toList(),
              onChanged: (value) => setState(() => _selectedTag = value),
            ),
            if (rfid.tags.isNotEmpty) ...[
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () {
                        final best = _pickTagByRssi(rfid, strongest: true);
                        if (best != null) setState(() => _selectedTag = best.epc);
                      },
                      icon: const Icon(Icons.near_me_outlined, size: 18),
                      label: const Text('Legnagyobb RSSI'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () {
                        final worst = _pickTagByRssi(rfid, strongest: false);
                        if (worst != null) setState(() => _selectedTag = worst.epc);
                      },
                      icon: const Icon(Icons.south_west_outlined, size: 18),
                      label: const Text('Legkisebb RSSI'),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildQuickEpcWrite(RfidService rfid) {
    final canWrite = rfid.isConnected && _selectedTag != null && !_isLoading;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.qr_code_2, color: AppTheme.secondaryColor, size: 20),
                SizedBox(width: 8),
                Text('Gyors EPC írás (szöveg)', style: TextStyle(fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _epcController,
              decoration: InputDecoration(
                labelText: 'Új EPC szöveg',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                filled: true,
                fillColor: AppTheme.darkSurface.withOpacity(0.5),
              ),
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: canWrite && _epcController.text.trim().isNotEmpty ? _writeEpcText : null,
                icon: const Icon(Icons.publish_outlined),
                label: const Text('EPC írás'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMemoryBankSelector() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.memory, color: AppTheme.secondaryColor, size: 20),
                SizedBox(width: 8),
                Text('Memória terület', style: TextStyle(fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              children: _memBanks.map((bank) => ChoiceChip(
                label: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(bank['icon'], size: 16),
                    const SizedBox(width: 4),
                    Text(bank['name']),
                  ],
                ),
                selected: _selectedMemBank == bank['id'],
                selectedColor: AppTheme.primaryColor.withOpacity(0.3),
                onSelected: (_) => setState(() => _selectedMemBank = bank['id']),
              )).toList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAddressConfig() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.pin, color: AppTheme.accentColor, size: 20),
                SizedBox(width: 8),
                Text('Cím beállítások', style: TextStyle(fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    decoration: InputDecoration(
                      labelText: 'Kezdőcím (Word)',
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                      filled: true,
                      fillColor: AppTheme.darkSurface.withOpacity(0.5),
                    ),
                    keyboardType: TextInputType.number,
                    controller: TextEditingController(text: '$_startAddr'),
                    onChanged: (v) => _startAddr = int.tryParse(v) ?? 2,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: TextField(
                    decoration: InputDecoration(
                      labelText: 'Hossz (Word)',
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                      filled: true,
                      fillColor: AppTheme.darkSurface.withOpacity(0.5),
                    ),
                    keyboardType: TextInputType.number,
                    controller: TextEditingController(text: '$_length'),
                    onChanged: (v) => _length = int.tryParse(v) ?? 6,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPasswordField() {
    return TextField(
      controller: _passwordController,
      decoration: InputDecoration(
        labelText: 'Jelszó (hex)',
        prefixIcon: const Icon(Icons.key),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        filled: true,
        fillColor: AppTheme.darkCard,
      ),
    );
  }

  Widget _buildContentField() {
    return TextField(
      controller: _contentController,
      decoration: InputDecoration(
        labelText: 'Tartalom (hex)',
        prefixIcon: const Icon(Icons.edit),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        filled: true,
        fillColor: AppTheme.darkCard,
        hintText: 'pl. E2003412...',
      ),
      maxLines: 2,
    );
  }

  Widget _buildActionButtons(RfidService rfid) {
    final canOperate = rfid.isConnected && _selectedTag != null && !_isLoading;
    
    return Row(
      children: [
        Expanded(
          child: ElevatedButton.icon(
            onPressed: canOperate ? _readMemory : null,
            icon: _isLoading 
              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
              : const Icon(Icons.download),
            label: const Text('Olvasás'),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.secondaryColor,
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: ElevatedButton.icon(
            onPressed: canOperate && _contentController.text.isNotEmpty ? _writeMemory : null,
            icon: const Icon(Icons.upload),
            label: const Text('Írás'),
            style: ElevatedButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildResultCard() {
    return Card(
      color: _isSuccess ? AppTheme.accentColor.withOpacity(0.2) : AppTheme.errorColor.withOpacity(0.2),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(
              _isSuccess ? Icons.check_circle : Icons.error,
              color: _isSuccess ? AppTheme.accentColor : AppTheme.errorColor,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                _result!,
                style: TextStyle(
                  color: _isSuccess ? AppTheme.accentColor : AppTheme.errorColor,
                  fontFamily: 'monospace',
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _stringToHex(String str) {
    return str.codeUnits.map((c) => c.toRadixString(16).padLeft(2, '0')).join().toUpperCase();
  }

  RfidTag? _pickTagByRssi(RfidService rfid, {required bool strongest}) {
    if (rfid.tags.isEmpty) return null;
    final tags = rfid.tags;
    RfidTag best = tags.first;
    for (final t in tags.skip(1)) {
      if (strongest) {
        if (t.rssi > best.rssi) best = t;
      } else {
        if (t.rssi < best.rssi) best = t;
      }
    }
    return best;
  }

  Future<void> _writeEpcText() async {
    if (_selectedTag == null) return;
    setState(() { _isLoading = true; _result = null; });

    final rfid = context.read<RfidService>();
    final newEpcHex = _stringToHex(_epcController.text.trim());
    final success = await rfid.writeEpc(
      _selectedTag!,
      newEpcHex,
      password: _passwordController.text,
    );

    setState(() {
      _isLoading = false;
      _isSuccess = success;
      _result = success ? 'EPC írás sikeres!' : 'EPC írási hiba';
    });
  }

  Future<void> _readMemory() async {
    if (_selectedTag == null) return;
    setState(() { _isLoading = true; _result = null; });
    
    final rfid = context.read<RfidService>();
    final result = await rfid.readMemory(
      _selectedTag!,
      _selectedMemBank,
      _startAddr,
      _length,
      password: _passwordController.text,
    );

    setState(() {
      _isLoading = false;
      _isSuccess = result != null;
      _result = result ?? 'Olvasási hiba';
      if (result != null) {
        _contentController.text = result;
      }
    });
  }

  Future<void> _writeMemory() async {
    if (_selectedTag == null) return;
    setState(() { _isLoading = true; _result = null; });

    final rfid = context.read<RfidService>();
    final success = await rfid.writeMemory(
      _selectedTag!,
      _selectedMemBank,
      _startAddr,
      _contentController.text,
      length: _length,
      password: _passwordController.text,
    );

    setState(() {
      _isLoading = false;
      _isSuccess = success;
      _result = success ? 'Írás sikeres!' : 'Írási hiba';
    });
  }
}
