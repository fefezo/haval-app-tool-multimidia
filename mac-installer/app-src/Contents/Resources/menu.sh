#!/bin/bash
# Haval Installer - menu principal (executado dentro do Terminal).
# Antes de continuar: confirme que o MacBook esta conectado ao WiFi do carro.
set -u
cd "$(dirname "$0")" || exit 1

clear
echo "=============================================="
echo "          HAVAL INSTALLER - H6"
echo "=============================================="
echo ""
echo "  O MacBook deve estar conectado ao WiFi do"
echo "  carro (hotspot da central, 192.168.33.x)."
echo ""
echo "  1) Instalacao completa (primeira vez no carro)"
echo "  2) Atualizacao rapida  (mesma assinatura, so o APK)"
echo "  3) Teste offline      (verifica arquivos + servidor, sem o carro)"
echo "  4) Simulacao completa (mock do carro - dry-run, sem hotspot)"
echo ""
printf "  Escolha 1 a 4 e tecle ENTER: "
IFS= read -r choice
echo ""

case "$choice" in
  2) exec bash ./install-macos.sh --update ;;
  3) exec bash ./install-macos.sh --selftest ;;
  4) exec bash ./install-macos.sh --mock ;;
  *) exec bash ./install-macos.sh ;;
esac
