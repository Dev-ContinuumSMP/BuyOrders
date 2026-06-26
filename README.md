<img width="2560" height="1369" alt="Minecraft_ 1 21 11 - Multiplayer (3rd-party Server) 18_06_2026 2_52_15 pm" src="https://github.com/user-attachments/assets/0b12f645-b247-428b-b14f-4015ab67658e" />
<img width="2560" height="1369" alt="Minecraft_ 1 21 11 - Multiplayer (3rd-party Server) 15_06_2026 9_12_53 pm" src="https://github.com/user-attachments/assets/cbd8f581-78d7-4ba8-819e-43850317db91" />
<img width="2560" height="1369" alt="Minecraft_ 1 21 11 - Multiplayer (3rd-party Server) 15_06_2026 7_05_15 pm" src="https://github.com/user-attachments/assets/28000171-184b-446b-b9ef-c5b5774f0472" />
AGAuctions
AGAuctions is a complete player marketplace plugin for Paper and Folia servers.
It combines a modern auction house with buy orders, so players can both list what they have and post demand for what they need.

The plugin is built for real server use: fast GUI workflows, configurable messages and layouts, secure custom item handling, and reliable H2-backed persistence.

Features
Full auction house experience under the ah command
Buy-now listings and timed auction listings with bidding support
Buy orders for materials and items in hand
Offline-safe collection flow for pending deliveries
Secure custom item matching using PersistentDataContainer identity keys
Optional strict identity plus meta matching for stronger anti-spoof protection
Blacklisted item rules for both listings and buy-order creation
H2 database persistence for listings and orders
Vault support for economy integration
Extensive YAML configuration for GUIs, locale, feature matrix, webhooks, and item grouping
Commands
/ah
/ah sell <price> [amount]
/ah auction <startPrice> [duration] [amount]
/ah buy <id>
/ah bid <id> <amount>
/ah claim
/ah orders [material|claim]
/ah buyorder <material> <amount> <priceEach>
/orders [material|collect|claim|reload] (legacy access)
/buyorder <material> <amount> <priceEach> (legacy alias)
Permissions
buyorders.use
buyorders.ah.use
buyorders.ah.sell
buyorders.create
buyorders.admin
Why servers use AGAuctions
AGAuctions makes player trading feel alive.
Sellers can move stock quickly, buyers can post demand without waiting online, and staff keep full control through configurable rules and admin tools.
