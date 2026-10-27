# Features - Lista de Funcionalidades

## Tab: Configurações

### Segurança e Janelas

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Fechar janela ao desligar o veículo | Toggle | Fecha automaticamente todas as janelas quando o motor é desligado (DMS state = 0) |
| Fechar janela ao recolher retrovisores | Toggle | Sincroniza fechamento das janelas com o recolhimento dos retrovisores (só quando parado em P) |
| Fechar teto solar ao desligar | Toggle | Fecha o teto solar automaticamente quando o motor é desligado |
| Fechar teto solar ao recolher retrovisores | Toggle | Fecha o teto solar ao recolher retrovisores (só quando parado em P) |
| Fechar cortina do teto solar | Toggle | Fecha a cortina/shade do teto solar automaticamente quando o teto solar é fechado (aguarda 5s) |
| Fechar janelas com velocidade | Toggle + Slider | Fecha todas as janelas ao atingir a velocidade configurada. Reseta quando velocidade cai abaixo de 10 km/h |
| Fechar teto solar com velocidade | Toggle + Slider | Fecha o teto solar ao atingir a velocidade configurada. Reseta quando velocidade cai abaixo de 10 km/h |

### Ar-Condicionado

| Feature | Tipo | Descrição |
|---------|------|-----------|
| A/C no máximo ao ligar o carro | Toggle + Slider + Dropdown | Quando a temperatura interna supera o limite configurado, liga o A/C no máximo (fan 7, temp 16°C, sync on). Suaviza gradualmente conforme esfria. Configurável: temperatura de disparo, temperatura alvo, timeout |
| Ligar ventilação do banco com A/C ligado | Toggle | Ativa a ventilação do banco do motorista no nível máximo (3) quando o A/C é ligado; desativa quando o A/C é desligado |

### Monitoramento

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Manter desativado monitoramento de distrações | Toggle | Força desativação do DMS (Driver Monitoring System). Re-desativa automaticamente se o sistema tentar reativar |
| Desativar AVAS | Toggle | Força desativação do AVAS (Acoustic Vehicle Alerting System - som do carro elétrico). Re-desativa se o sistema tentar reativar |
| Desativar câmera AVM quando parado | Toggle | Desliga automaticamente a câmera de visão 360° quando o veículo está parado (velocidade ≤ 0 e marcha ≠ Ré) |

### Cluster (Painel de Instrumentos)

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Habilitar menu customizado no cluster | Toggle | Exibe um menu customizado no cluster controlado pelas teclas de navegação do volante (UP/DOWN/ENTER/BACK) |

No widget de A/C do cluster, os controles que mudam com as teclas UP/DOWN do volante
mostram a direção da mudança:

- **Fan e temperatura:** dois chevrons (cima/baixo) ao lado do valor. O chevron da
  direção usada acende e o número entra deslizando daquele lado. A direção é inferida
  comparando o valor anterior com o novo, porque o carro envia apenas o valor final.
- **Circulação (interna/externa):** uma chave física de duas posições, com o knob
  deslizando para a posição ativa (externa em cima, interna embaixo).
- **Modo padrão:** os chevrons ao lado do check indicam que UP/DOWN aplica o perfil salvo.

### Conectividade

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Desligar Bluetooth ao desligar | Toggle | Desativa o Bluetooth quando o carro é desligado. Reativa automaticamente ao religar se estava ativo antes |
| Desligar ponto de acesso ao desligar | Toggle | Desativa o hotspot Wi-Fi quando o carro é desligado |

### Volante

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Habilitar botões personalizados no volante | Toggle + 2 Dropdowns | Remapeia os 2 botões customizáveis do volante. Ações disponíveis para cada botão: |

**Ações dos botões do volante:**

| Ação | Descrição |
|------|-----------|
| Padrão da multimídia | Mantém o comportamento original |
| Alterar nível de regeneração | Cicla entre Baixo → Normal → Alto |
| Alterar modo de potência | Cicla entre HEV → Prior. EV → Modo EV |
| Alternar ionizador | Liga/desliga ionizador do A/C |
| Alternar condução com um pedal | Liga/desliga one-pedal driving |
| Abrir aplicativo | Abre um app configurado pelo package name |
| Alternar câmera AVM | Liga/desliga o modo de desabilitar câmera quando parado |
| Abrir câmera uma vez | Abre/fecha a câmera AVM sem a lógica de auto-desligar |

### Brilho

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Ajustar brilho automaticamente | Toggle + Time Pickers + 2 Sliders | Ajusta o brilho da tela baseado no horário (dia/noite). Configurável: horário de início/fim da noite, nível de brilho diurno (1-10), nível de brilho noturno (1-10) |

### Volume

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Definir volume inicial | Toggle + Slider | Define um volume fixo ao ligar o veículo (0-40) |

### Avançado (só visível com `advanceUse`)

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Bypass de Verificação | Toggle | Ignora a verificação de integridade da instalação (UID check). Só visível quando a verificação falhou |

## Tab: Consumo

Duas visões, selecionadas por abas internas:

### Viagens

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Registro por ciclo | Automático | Cria uma viagem por ciclo liga→desliga, com distância, consumo em km/L e odômetro. Persistido em JSON |
| Gráfico de dois eixos | Visualização | Mostra km/L e distância (km) por viagem, com legenda e escalas separadas |
| Tabela de viagens | Lista | Data, distância e consumo por viagem, do mais recente para o mais antigo |

### Tanques

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Detecção de abastecimento | Automático | Acompanha `remain_fuel_percentage`; um salto de 5 pontos percentuais (com cooldown de 10 min) fecha o tanque atual e abre um novo. A autonomia e o nível do momento do abastecimento viram a referência do tanque |
| Tanque atual | Card ao vivo | Distância rodada, consumo estimado, nível atual, autonomia no início/agora e litros estimados do abastecimento |
| Comparativo por posto | Relatório | Agrupa os tanques fechados pelo nome do posto e mostra o consumo médio ponderado (km total ÷ litros estimados), do melhor para o pior |
| Histórico de tanques | Lista | Data, posto, distância e km/L de cada tanque; toque para nomear/editar o posto do abastecimento |
| Limpar histórico | Botão + confirmação | Apaga tanques e nomes de postos; o acompanhamento recomeça na próxima leitura de nível |

> O carro não publica litros abastecidos. O volume é estimado pela variação do nível
> de combustível × 60 L de capacidade do tanque. É uma estimativa, mas usa a mesma
> régua para todos os tanques — o que permite comparar um posto com outro. Nomear o
> posto é manual: use o mesmo nome para o mesmo lugar.

## Tab: Telas

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Projetor do painel | Toggle | Ativa o sistema de projetores no cluster. Desabilitar desativa também os itens abaixo |
| Aviso de revisão | Toggle (depende de Projetor) | Exibe aviso de manutenção no Display 3. Quando ativo, mostra sub-controles: |
| — Próxima KM | Campo numérico + botão Resetar | Quilometragem da próxima revisão. Resetar calcula odômetro atual + 12.000 km |
| — Próxima data | Date Picker + botão Resetar | Data da próxima revisão. Resetar define para 1 ano a partir de hoje |
| Integração customizada de mídia | Toggle (depende de Projetor) | Ativa o projetor WebView no Display 1 (menu, A/C, gráficos). Desabilita o app nativo `com.beantechs.multidisplay` |

> Nota: A chave `ENABLE_INSTRUMENT_EV_BATTERY_PERCENTAGE` existe no código e é usada pelo `InstrumentProjector`, mas atualmente não possui toggle na UI.

## Tab: Valores Atuais

Tela de debug que mostra em tempo real todos os valores das propriedades monitoradas do veículo.

- **Campo de busca** para filtrar propriedades por nome
- Exibe pares chave/valor em cards com atualização em tempo real via `IDataChanged`
- **Modo avançado:** Botão "Configurar" abre diálogo com checkbox de todos os `CarConstants`, permitindo adicionar/remover propriedades do monitoramento (salvo como `carMonitorProperties` StringSet)
- Clicar em um card permite editar manualmente o valor da propriedade

## Tab: Instalar Apps

Loja de apps integrada com grid de 4 colunas.

- **Instalar via URL**: Campo de texto + botão para baixar e instalar APK de qualquer URL, com barra de progresso
- **Grid de apps**: Busca lista de apps disponíveis do GitHub. Cada card mostra nome, versão disponível, versão instalada e botões de Instalar/Atualizar/Desinstalar
- **Sennuopu DP-X10 embutido**: o APK do app de equalização do processador de som viaja dentro do Impulse (`res/raw/sennuopu_dp_x10.apk`) e aparece no grid mesmo sem internet. O botão Instalar copia o APK local e abre o instalador do Android; não há download
- Apps são ordenados: precisa atualizar primeiro, depois disponíveis, depois já atualizados

## Tab: Informações

- **Status do sistema**: Indicador de instalação correta, estado ativo/inativo, métricas de tempo de boot/inicialização
- **Versão do app**: Clicar 5 vezes na versão ativa o **Modo Avançado** (easter egg que mostra a tab Frida Hooks)
- **Buscar Atualizações**: Verifica releases no GitHub e oferece download + instalação
- **Abrir Configurações do Android**: Atalho para as configurações do sistema
- **Card de contribuição**: QR code + chave PIX para doações

## Tab: Frida Hooks (modo avançado)

| Feature | Tipo | Descrição |
|---------|------|-----------|
| Habilitar Frida Hooks | Toggle | Ativa a injeção de scripts Frida em processos do sistema. Exibe diálogo de confirmação |
| Hook System Server | Toggle | Injeta script no `system_server` (com delay de 10s) |
| Injetar Código Manual | Botão | Permite injetar scripts Frida manualmente em processos específicos |
