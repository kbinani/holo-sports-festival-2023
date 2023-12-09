# holo-sports-festival-2023

Minecraft [#ホロライブ大運動会2023](https://twitter.com/hashtag/%E3%83%9B%E3%83%AD%E3%83%A9%E3%82%A4%E3%83%96%E5%A4%A7%E9%81%8B%E5%8B%95%E4%BC%9A2023) で行われた各種ミニゲームの動作を再現するための Paper カスタマイズサーバー、Paper プラグイン、およびリソースパックです。

## ミニゲーム一覧
以下のミニゲームがプレイ可能です。
- 上を目指せ! HoloUp
- 騎馬戦
- 姫護衛レース
- 春夏秋冬リレー

## ビルド方法

本プロジェクトのルートディレクトリで `make` とするとビルドが行われます。

- Paper カスタマイズサーバー
  - `server/Paper/build/libs` 配下にビルド結果の jar が生成されます。
  - Paper カスタマイズサーバーだけビルドする場合は `make server` として下さい。
- Paper プラグイン
  - `plugin/build/libs` 配下にビルド結果の jar が生成されます。
  - Paper プラグインだけビルドする場合は `make plugin` として下さい。
- リソースパック
  - `resourcepack` 配下にビルド結果の zip が生成されます。
  - リソースパックだけビルドする場合は `make resourcepack` として下さい。

## 謝辞
- リソースパックの画像は [いらすとや](https://www.irasutoya.com/) 様のものを利用させていただきました。
