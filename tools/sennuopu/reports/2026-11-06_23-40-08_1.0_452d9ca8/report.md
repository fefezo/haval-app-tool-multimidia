# Sennuopu DP-X10 — análise estática

Gerado em `2026-11-07T02:40:09+00:00` por `tools/sennuopu/inspect_apk.py`.

## Resumo

| Campo | Valor |
| --- | --- |
| APK | `Sennuopu DP-X10 Android APP_V0.0.4_220228.apk` |
| SHA-256 do APK | `452d9ca8180f314ac9b59360cc41acae772e8e41a054cdc1e4adfe635aa364e0` |
| Tamanho do APK | 7977462 bytes |
| Pacote | `com.nisson.x10.cne` |
| Versão | `1.0` (versionCode `1`) |
| libX5.so | `lib/armeabi-v7a/libX5.so` |
| SHA-256 da lib | `47337439cb2772fb56be393fc149a0dded81f25bb08a69c1a767f89f39c9c797` |
| Tamanho da lib | 1606808 bytes |
| ELF | 32-bit, machine `40` |
| Entradas `.dynsym` | 3270 (2609 definidas, 661 importadas) |
| Tabela de comandos | RVA `0x9269c`, 751 IDs não nulos |
| Âncoras da tabela | OK |
| Baseline | `report.json + attribute-map.csv` |
| Diff da tabela | 0 alterados, 0 adicionados, 0 removidos |
| `libX5.so` vs baseline | igual |

## Tabela de comandos

- Fonte: `auto`
- RVA: `0x9269c` (offset no arquivo: `0x9269c`)
- IDs não nulos: 751; comandos únicos: 751
- Páginas: `0x00`=57, `0x01`=82, `0x02`=82, `0x03`=27, `0x04`=24, `0x05`=108, `0x06`=106, `0x07`=106, `0x08`=108, `0x09`=29, `0x0a`=22
- Candidatos encontrados pela auto-localização: 1

## Mudanças na tabela vs baseline

Nenhuma mudança na tabela de comandos.

## Símbolos-chave

| Função | Endereço | Tamanho | SHA-256 dos bytes |
| --- | --- | --- | --- |
| `mAssemblePackge` | `0x43420` | 592 | `97cc7f34f6eda19a…` |
| `mCheckPackge` | `0x432b8` | 88 | `6dd19176eb5006e6…` |
| `mBuildResponse` | `0x43310` | 196 | `eb8d1153058df120…` |
| `mCalCheck` | `0x433f0` | 48 | `2949a3b730372bb4…` |
| `mSendRequest` | `0x40e38` | 228 | `7fb0a92646d0afd7…` |
| `mRecvData` | `0x40f24` | 240 | `a6757e69163eeac0…` |
| `mWriteData_char` | `0x4176a` | 86 | `4400f483425a4406…` |
| `mWriteData_qba` | `0x417c0` | 12 | `2f5f14332d9d49bc…` |
| `mDataSend` | `0x8c484` | 56 | `120916aeffdb4047…` |
| `mWrite` | `0x41990` | 240 | `15587b69468be38b…` |
| `mBleRecvData` | `0x41a8e` | 66 | `cc2b2d662275b9cc…` |
| `mConnectDevice` | `0x417fc` | 260 | `1e1281589235e6dc…` |
| `mSetCommunicationUuids` | `0x417d2` | 42 | `5fcacbdfa4379e4e…` |
| `mDealServiceDiscovery` | `0x41ad6` | 194 | `bb94471b3a77e7a5…` |
| `mDealServiceStateChanged` | `0x41b98` | 280 | `1e2be56715bc7471…` |
| `mDealNotify` | `0x67718` | 40 | `74ca244563f9f1dc…` |
| `mSerializeData` | `0x56232` | 150 | `0302713589199431…` |
| `mDeSerializeData` | `0x547f2` | 126 | `b548802bece6fd95…` |

## Strings de interesse

- UUIDs: `0000AE00-0000-1000-8000-00805F9B34FB`, `0000AE02-0000-1000-8000-00805F9B34FB`, `0000AE10-0000-1000-8000-00805F9B34FB`
- Parâmetros de sistema (38): `System_AUXSignalGain`, `System_BluetoothSignalGain`, `System_CUL`, `System_DEQPass`, `System_DEQReset`, `System_DelayShowUint`, `System_DownloadScenes`, `System_EQPass`, `System_EQReset`, `System_FirmwareVersion`, `System_HPSIA`, `System_HPSIAAux`, `System_HPSIABluetooth`, `System_HPSIAUSB`, `System_LevelSelection`, `System_LimiterAttack`, `System_LimiterRelease`, `System_LimiterThreshold`, `System_Link1To2`, `System_Link1To4`, `System_Link3To4`, `System_Link5To6`, `System_LinkFree`, `System_LoadMode`, `System_NoiseGateStart`, `System_NoiseGateThreshold`, `System_OutputRelate`, `System_Reset`, `System_ResetOutput`, `System_SaveFile`, `System_SoundSource`, `System_SoundSourceInput`, `System_SourcePreference`, `System_SystemMute`, `System_SystemVolumn`, `System_USBSoundCarGain`, `System_UpdateFirmware`, `System_UploadScenes`
- Canais: 48 nomes × 8 canais = 384 strings (`OutputChannelN_...`)
- Nomes de canal: `ChannelDelay`, `ChannelGain`, `ChannelMute`, `ChannelPhase`, `DEQ1`, `DEQ2`, `DEQ3`, `DEQWorkMode1`, `DEQWorkMode2`, `DEQWorkMode3`, `EQ1`, `EQ10`, `EQ11`, `EQ12`, `EQ13`, `EQ14`, `EQ15`, `EQ16`, `EQ17`, `EQ18`, `EQ19`, `EQ2`, `EQ20`, `EQ21`, `EQ22`, `EQ23`, `EQ24`, `EQ25`, `EQ26`, `EQ27`, `EQ28`, `EQ29`, `EQ3`, `EQ30`, `EQ31`, `EQ4`, `EQ5`, `EQ6`, `EQ7`, `EQ8`, `EQ9`, `EQ_PASS`, `HPF`, `InputRouting`, `LPF`, `Sensitivity1`, `Sensitivity2`, `Sensitivity3`
- Perfis/cenas: `Center`, `Echo`, `MainSound`, `MicA`, `Music`, `Reverb`, `Surround`, `Ultralow`
- Símbolos com `ParamsConfig`: 18

A lista completa de strings está em `strings-interesting.txt`.

## Próximos passos

- A tabela foi localizada automaticamente; confira o RVA no `report.json`.
- Se o diff não mudou nada relevante, o protocolo continua igual; uma nova captura Frida é opcional.
- Para embutir esta versão no Impulse, substitua `app/src/main/res/raw/sennuopu_dp_x10.apk` de propósito.

