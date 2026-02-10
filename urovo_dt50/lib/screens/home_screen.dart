import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';
import '../models/rfid_tag.dart';
import '../services/rfid_service.dart';
import '../theme/app_theme.dart';
import '../widgets/scan_button.dart';
import 'settings_screen.dart';
import 'write_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<RfidService>().connect();
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        toolbarHeight: 48,
        title: Consumer<RfidService>(
          builder: (context, rfid, _) => Row(
            children: [
              Container(
                width: 12,
                height: 12,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: rfid.isConnected ? Colors.green : Colors.red,
                ),
              ),
              const SizedBox(width: 10),
              const Text('Urovo DT50', style: TextStyle(fontSize: 18)),
            ],
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings_outlined, size: 22),
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const SettingsScreen()),
            ),
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppTheme.primaryColor,
          labelColor: AppTheme.primaryColor,
          unselectedLabelColor: Colors.grey,
          tabs: const [
            Tab(text: 'Scan'),
            Tab(text: 'Write'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: const [
          ScanTab(),
          WriteScreen(),
        ],
      ),
    );
  }
}

class ScanTab extends StatelessWidget {
  const ScanTab({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<RfidService>(
      builder: (context, rfid, _) {
        return Column(
          children: [
            // Compact header: stats + actions
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: AppTheme.darkSurface,
                border: Border(bottom: BorderSide(color: Colors.grey.shade800)),
              ),
              child: Row(
                children: [
                  // Stats inline
                  Text(
                    '${rfid.totalReads} reads • ${rfid.tags.length} tags',
                    style: const TextStyle(fontSize: 13, color: Colors.grey),
                  ),
                  const Spacer(),
                  // Action buttons
                  IconButton(
                    visualDensity: VisualDensity.compact,
                    icon: Icon(Icons.delete_outline, size: 20, color: rfid.tags.isEmpty ? Colors.grey : AppTheme.errorColor),
                    onPressed: rfid.tags.isEmpty ? null : rfid.clearTags,
                  ),
                  IconButton(
                    visualDensity: VisualDensity.compact,
                    icon: Icon(Icons.share_outlined, size: 20, color: rfid.tags.isEmpty ? Colors.grey : AppTheme.secondaryColor),
                    onPressed: rfid.tags.isEmpty ? null : () {
                      Share.share(rfid.getTagsAsText(), subject: 'RFID Scan');
                    },
                  ),
                ],
              ),
            ),
            // Tag list - main focus
            Expanded(
              child: rfid.tags.isEmpty
                  ? const Center(child: Text('Nyomd meg a SCAN gombot', style: TextStyle(color: Colors.grey)))
                  : ListView.builder(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      itemCount: rfid.tags.length,
                      itemBuilder: (context, index) {
                        final tag = rfid.tags[index];
                        return _CompactTagTile(tag: tag, isFirst: index == 0);
                      },
                    ),
            ),
            // Scan button at bottom
            SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: ScanButton(),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _CompactTagTile extends StatelessWidget {
  final RfidTag tag;
  final bool isFirst;

  const _CompactTagTile({required this.tag, required this.isFirst});

  @override
  Widget build(BuildContext context) {
    final decoded = tag.decodedText;
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: isFirst ? AppTheme.primaryColor.withOpacity(0.15) : AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(8),
        border: isFirst ? Border.all(color: AppTheme.primaryColor, width: 1) : null,
      ),
      child: Row(
        children: [
          // Tag info
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (decoded != null)
                  Text(decoded, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                Text(
                  tag.epcFormatted,
                  style: TextStyle(
                    fontFamily: 'monospace',
                    fontSize: decoded != null ? 11 : 13,
                    color: decoded != null ? Colors.grey : Colors.white,
                  ),
                ),
              ],
            ),
          ),
          // RSSI + count
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text('${tag.rssi} dBm', style: const TextStyle(fontSize: 12, color: Colors.grey)),
              Text('×${tag.readCount}', style: const TextStyle(fontSize: 11, color: Colors.grey)),
            ],
          ),
        ],
      ),
    );
  }
}
