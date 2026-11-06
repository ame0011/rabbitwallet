# AGENTS.md — RabbitWallet (MoneyWallet フォーク)

## コミットメッセージの規約

新規コミットは **Conventional Commits** で書く。履歴にある旧 `[Gradle] ...` / `[Bugfix] ...` 形式は
上流 MoneyWallet 由来のレガシーなので、今後は使わない。

### フォーマット

```
<type>(<scope>): <subject>

<body>

<footer>
```

- 1行目(`header`)は 72 文字以内
- `header` と `body` の間に空行を1つ入れる
- `subject` は日本語・文末に句点(。)なし
- 本文(`body`)は理由と内容を箇条書きで。1行 72 文字以内

### type(半角英小文字)

| type | 用途 | 例 |
|------|------|-----|
| `feat` | 新機能 | `feat(localization): 新規言語リソース 日本語 を追加` |
| `fix` | バグ修正 | `fix(currency): 64.99 が 64.98 に丸められる問題を修正` |
| `refactor` | 挙動を変えない再構成・改名 | `refactor(package): フォーク向けにパッケージ名とアプリ名を RabbitWallet へ変更` |
| `build` | Gradle / 依存関係 / ビルド設定 | `build(gradle): JCenter shutdown に対応したリポジトリ設定` |
| `test` | テスト追加・修正 | `test(database): 端末言語が英語でない場合のテストケースを修正` |
| `docs` | ドキュメントのみ | `docs(readme): ストアリンクを RabbitWallet の ID に更新` |
| `chore` | 上記いずれにも当てはまらない雑務 | `chore(assets): 廃止された Google+ リンクを削除` |
| `perf` | パフォーマンス改善 | `perf(chart): 円グラフの再描画回数を削減` |
| `ci` / `style` | CI 設定 / 整形のみ(挙動変更不要) | — |

### scope(任意、半角英小文字)

既存の領域名を使う: `gradle`, `package`, `manifest`, `localization`, `strings`, `ui`, `database`,
`backup`, `sync`, `assets`, `test`, `map`, `currency` など。複数領域にまたがる場合は省略可。

### 破壊的変更

- `type`/`scope` の直後に `!` を付ける: `refactor(package)!: applicationId を変更`
- または footer に `BREAKING CHANGE: <説明>` を書く

### その他

- マージコミットは作らない(線形履歴を維持。上流取り込みは rebase)
- 1コミット = 1論理変更。無関係な整形やリネームは分離する
- 既存の日本語コミット(`feat(localization): ...`)を手本にする
