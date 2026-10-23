# Jarvis Caseiro — Android

Projeto completo de um "Jarvis" doméstico: um app Android que escuta comandos
enviados pelo Firebase Realtime Database e executa ações no celular (câmera,
alarme, apito, lanterna, notificações, etc). Compilado 100% pelo GitHub
Actions — **você não precisa instalar o Android Studio**.

## ⚠️ Decisão importante sobre a câmera (leia antes de usar)

Os comandos `tirar_foto_traseira`, `tirar_foto_frontal` e `tirar_foto_ambas`
**abrem uma tela visível no celular** (acendem a tela, mostram o preview da
câmera e uma notificação) antes de capturar a foto. Isso foi uma decisão de
propósito: um mecanismo que tira fotos de forma totalmente silenciosa, com a
tela bloqueada e sem indicação nenhuma pra quem está com o celular na mão, é
tecnicamente idêntico ao usado por aplicativos de vigilância não autorizada
(stalkerware) — e esse pedaço específico eu não implementei, mesmo sendo o seu
próprio aparelho, porque o código fica igual em qualquer instalação.

Na prática, isso também bate com uma restrição real do Android: desde a versão
9, apps em segundo plano têm acesso bloqueado à câmera pelo próprio sistema.
Então mesmo tentando burlar isso, não haveria captura silenciosa confiável no
Android 13 — a tela precisa estar ativa e o app em primeiro plano de verdade.

## O que está implementado

| Categoria | Comandos |
|---|---|
| Câmera (visível, app em 1º plano) | `tirar_foto_traseira`, `tirar_foto_frontal`, `tirar_foto_ambas`, `abrir_camera` |
| Alarme | `alarme` (horário, atraso em minutos ou recorrente diário), `cancelar_alarme` |
| Som | `apitar` (com duração máxima de segurança), `parar_alarme` |
| Lanterna | `lanterna_on`, `lanterna_off` |
| Apps e links | `abrir_app` (só apps na whitelist), `abrir_url` |
| Notificações/mensagens | `notificacao`, `mensagem` |
| Voz | `falar` (TTS), `ouvir_comando` (reconhecimento de voz, visível ao usuário) |
| Status do aparelho | `status_bateria`, `status_rede` |
| Sistema | `modo_nao_perturbe` |

Toda execução atualiza `/respostas/{id}` no Firebase com `status`:
`recebido` → `executando` → `concluido` ou `erro`, para a página `jarvis.html`
acompanhar em tempo real.

## O que eu NÃO consegui testar/compilar aqui

Este ambiente onde o projeto foi gerado **não tem acesso à internet**, então eu
não baixei dependências nem rodei `gradle build` aqui — não dá pra simular isso
sem rede. Eu revisei o código manualmente (estrutura de pacotes, chaves
balanceadas, imports), mas a primeira compilação real e definitiva só vai
acontecer quando o GitHub Actions rodar, depois que você subir o repositório.
Se houver erro de compilação, o log do Actions vai mostrar exatamente onde —
me mande o log e eu corrijo.

## Passo a passo para colocar no ar

### 1. Configurações no console do Firebase (únicas ações manuais obrigatórias)

1. **Authentication → Sign-in method → Anonymous → Ativar.**
   As regras do banco (`database.rules.json`) agora exigem `auth != null` para
   ler/escrever — isso impede que qualquer pessoa na internet leia ou mande
   comandos pro seu celular só por conhecer a URL do banco. A autenticação
   anônima é gratuita e não pede login do usuário.
2. **Realtime Database → Regras**: cole o conteúdo de `database.rules.json`
   deste projeto e publique.
3. **Configurações do projeto → Seus apps → Adicionar app Android** (se ainda
   não existir um com `applicationId = com.jarviscaseiro.app`):
   - Baixe o `google-services.json` gerado.
   - Coloque esse arquivo em `app/google-services.json` no repositório (vai
     substituir o arquivo `app/google-services.json.EXEMPLO`, que é só um
     modelo de estrutura).
   - **Sem esse arquivo real, o workflow do GitHub Actions falha de propósito**
     (ver `.github/workflows/build-apk.yml`) em vez de compilar algo quebrado.

### 2. Subir para o GitHub

```bash
# dentro da pasta do projeto, após extrair o ZIP
git init
git remote add origin https://github.com/SEU_USUARIO/PROJETOJARVIS.git
git add .
git commit -m "Jarvis Caseiro completo"
git branch -M main
git push -u origin main --force
```
(`--force` só é necessário se o repositório `PROJETOJARVIS` já tiver conteúdo
antigo incompatível; se preferir preservar o histórico antigo, suba como uma
branch separada e migre manualmente.)

### 3. Baixar o APK

1. Vá na aba **Actions** do repositório no GitHub.
2. Abra a execução do workflow "Compilar APK do Jarvis Caseiro".
3. Quando terminar (ícone verde), abra a seção **Artifacts** e baixe
   `jarvis-caseiro-debug-apk`.
4. Transfira o `.apk` pro celular (Drive, cabo USB, etc) e instale manualmente
   (ative "instalar de fontes desconhecidas" quando o Android pedir).

### 4. No celular, depois de instalar

Abra o app e conceda as permissões pedidas. Na tela principal você verá:
- O **ID do dispositivo** gerado automaticamente (use esse valor como
  `destino` nos comandos vindos da página web).
- Quais permissões ainda faltam conceder.
- Toque em **"Iniciar serviço"** para começar a escutar comandos.

Permissões que o Android 13 vai pedir (ou que você precisa conceder manualmente):
- Câmera, Microfone, Notificações — pedidas automaticamente pelo app.
- **Alarmes exatos** (`SCHEDULE_EXACT_ALARM`) — em alguns aparelhos precisa ser
  liberada manualmente em Configurações → Apps → Jarvis Caseiro → Alarmes e
  lembretes.
- **Acesso à política de notificações** (para `modo_nao_perturbe`) — Configurações
  → Apps especiais → Acesso ao Não Perturbe → Jarvis Caseiro.
- **Ignorar otimização de bateria** — recomendado manualmente (Configurações →
  Bateria → Jarvis Caseiro → Sem restrições), senão o fabricante do aparelho
  pode matar o serviço em segundo plano depois de um tempo. Isso varia muito
  entre marcas (Xiaomi, Samsung, Motorola têm telas próprias de "app em
  segundo plano" além da configuração padrão do Android).

### 5. Adaptar a página `jarvis.html`

Veja `docs/jarvis-html-integracao.md` — ele explica exatamente como autenticar
e como montar cada comando no mesmo formato que o app espera.

## Limitações conhecidas (honestas, não maquiadas)

- **Câmera com tela bloqueada**: não implementado, de propósito (ver seção
  acima). A captura acontece com a tela acesa e o app em primeiro plano.
- **Apito/som**: usa o canal de alarme do próprio aparelho; em celulares com
  "modo silencioso absoluto" ativado pelo usuário, o som pode não tocar — isso
  é intencional (o projeto não desativa os controles de volume do usuário).
- **Alarme exato e serviço em segundo plano**: fabricantes como Xiaomi,
  Samsung e Motorola têm gerenciadores de bateria próprios que podem matar o
  serviço mesmo com as permissões do Android concedidas corretamente. Não há
  solução 100% garantida para isso — apenas mitigação (serviço em primeiro
  plano + notificação persistente + pedido de ignorar otimização de bateria).
- **`abrir_app`**: só funciona para pacotes que você mesmo colocar na whitelist
  (ainda não há tela de configuração dessa lista na interface — por ora,
  defina via `Prefs.appsPermitidos` no código ou me peça a tela de
  configuração como próximo passo).
- **Reinicialização do aparelho**: alarmes são reagendados automaticamente; o
  serviço só volta a ligar sozinho se estava ativo antes do reboot.
- **Compilação real**: ainda não confirmada neste ambiente (sem internet) —
  confirmação definitiva vem do primeiro run do GitHub Actions.

## Estrutura do projeto

```
JarvisCaseiro/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json.EXEMPLO   <- você substitui pelo real
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/jarviscaseiro/app/
│       │   ├── MainActivity.kt, JarvisApplication.kt, Config.kt
│       │   ├── service/JarvisForegroundService.kt
│       │   ├── firebase/FirebaseManager.kt
│       │   ├── comandos/ (Comando.kt, CommandProcessor.kt)
│       │   ├── camera/ (CameraController.kt, CameraCaptureActivity.kt)
│       │   ├── alarme/ (AlarmScheduler.kt, AlarmReceiver.kt, AlarmActivity.kt, AlarmStore.kt)
│       │   ├── apito/ApitoManager.kt
│       │   ├── lanterna/LanternaController.kt
│       │   ├── voz/ (TtsManager.kt, SpeechManager.kt)
│       │   ├── status/StatusProvider.kt
│       │   ├── apps/AppLauncher.kt
│       │   ├── dnd/DndController.kt
│       │   ├── boot/BootReceiver.kt
│       │   └── util/Prefs.kt
│       └── res/ (layouts, strings, temas, ícones)
├── database.rules.json
├── docs/jarvis-html-integracao.md
├── .github/workflows/build-apk.yml
└── settings.gradle.kts / build.gradle.kts / gradle.properties
```
