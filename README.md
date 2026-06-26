# AGAuctions
<img width="1254" height="1254" alt="icon" src="https://github.com/user-attachments/assets/58775568-9023-4c85-ad71-a354ec4f7948" />


A polished auction house and buy-order marketplace plugin for Paper and Folia servers.

AGAuctions gives your economy a true two-way market:
- Sellers can list items for instant purchase or timed bidding.
- Buyers can post demand through buy orders and get filled by other players.

Built for live servers with secure custom-item handling, configurable GUIs, and reliable H2-backed storage.

## Why AGAuctions

- AH-first command flow centered around `/ah`
- Buy-now listings and timed auctions with bidding
- Buy orders for materials and in-hand items
- Offline-safe item collection and claim flows
- PDC-aware custom item identity matching
- Optional strict identity + meta validation to reduce spoofing
- Blacklist enforcement for listings and buy-order creation
- Vault economy support when available
- Folia-compatible runtime support
- Modular YAML configuration system

## Requirements

- Java 21
- Paper 1.21+ (Folia supported)

## Installation

1. Build the jar using Maven Wrapper.
2. Place the built jar in your server `plugins` directory.
3. Start the server once to generate plugin data and config files.
4. Edit configuration files to match your economy and GUI preferences.
5. Restart or reload the plugin.

Build commands:

```bash
# Windows
mvnw.cmd clean package

# Linux/macOS
./mvnw clean package
```

## Screenshots

### Auction House

| Auction House Overview | Statistics |
|---|---|
| <img width="352" height="440" alt="2026-06-26_04 11 02" src="https://github.com/user-attachments/assets/944696e9-9064-4a4f-b091-9086cf29d0be" />


| Filter Controls | Search Control |
|---|---|
| ![Auction House Filter](./docs/images/ah-filter.png) | ![Auction House Search](./docs/images/ah-search.png) |

| Claims Button | Close Button |
|---|---|
| ![Auction House Claims](./docs/images/ah-claims.png) | ![Auction House Close](./docs/images/ah-close.png) |

| Page Switch |
|---|
| ![Auction House Page Switch](./docs/images/ah-page-switch.png) |

### Orders

| Orders Overview | Collection Shortcut |
|---|---|
| ![Orders Overview](./docs/images/orders-overview.png) | ![Orders Collection Shortcut](./docs/images/orders-filter.png) |

| Search | Refresh |
|---|---|
| ![Orders Search](./docs/images/orders-search.png) | ![Orders Refresh](./docs/images/orders-refresh.png) |

| Close | Page Switch |
|---|---|
| ![Orders Close](./docs/images/orders-close.png) | ![Orders Page Switch](./docs/images/orders-page-switch.png) |

### Collection

| Collection Overview | Claim All |
|---|---|
| ![Collection Overview](./docs/images/collection-overview.png) | ![Collection Claim All](./docs/images/collection-claim-all.png) |

| Waiting Count | Back To Orders |
|---|---|
| ![Collection Waiting Count](./docs/images/collection-waiting.png) | ![Collection Back To Orders](./docs/images/collection-back-to-orders.png) |

## Commands

### Auction House

| Command | Description |
|---|---|
| `/ah` | Open the auction house |
| `/ah sell <price> [amount]` | Create a buy-now listing |
| `/ah auction <startPrice> [duration] [amount]` | Create a timed auction listing |
| `/ah buy <id>` | Buy a direct listing |
| `/ah bid <id> <amount>` | Place a bid on an auction listing |
| `/ah claim` | Claim auction outcomes and settlements |

### Buy Orders

| Command | Description |
|---|---|
| `/ah orders [material|collect|claim]` | Open buy orders from AH flow |
| `/ah order [material|collect|claim]` | Alias of `/ah orders` |
| `/ah buyorder <material> <amount> <priceEach>` | Create a material buy order from AH flow |
| `/ah buyorder hand <amount> <priceEach>` | Create a buy order for the held item from AH flow |
| `/ah buyorders <material> <amount> <priceEach>` | Alias of `/ah buyorder` for a cleaner AH command flow |
| `/ah buyorders hand <amount> <priceEach>` | Alias of `/ah buyorder hand` |
| `/buyorder <material> <amount> <priceEach>` | Legacy create command |
| `/buyorder hand <amount> <priceEach>` | Create a buy order for the held item |
| `/orders [material|collect|claim|reload]` | Legacy orders path |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `buyorders.use` | View and fill buy orders | `true` |
| `buyorders.ah.use` | Use auction house features | `true` |
| `buyorders.ah.sell` | Create auction listings | `true` |
| `buyorders.create` | Create buy orders | `true` |
| `buyorders.admin` | Admin controls (cancel/reload) | `op` |

## Configuration Files

AGAuctions is designed to be configured by server owners through modular YAML files:

- `config.yml`: Core behavior, economy provider, item matching, AH limits
- `guis.yml`: GUI slots, icons, lore, labels, and layout behavior
- `locale.yml`: Player-facing messages and language text
- `gui-feature-matrix.yml`: Per-profile GUI feature toggles
- `blacklisted-items.yml`: Material/name restrictions for safety rules
- `item-groups.yml`: Category-style grouping support
- `webhook-dispatch.yml`: Webhook templates and dispatch settings

## Data and Storage

- Persistence uses H2 database storage.
- Data files are stored under the plugin data folder.
- Legacy migration paths for older flat-file data are preserved where applicable.

## Release

- Current release: `1.0`
- Artifact: `AGAuctions-1.0.jar`

## Contributing and Support

Issues and pull requests are welcome.

When reporting a bug, include:
- Server software and version
- Java version
- Plugin version
- Reproduction steps
- Relevant logs

## License

See `LICENSE`.
