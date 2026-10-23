# Como a página `jarvis.html` deve falar com o app

O app Android e a página web usam exatamente a mesma estrutura no Realtime Database.
Como as regras (`database.rules.json`) agora exigem `auth != null`, **a página também
precisa se autenticar de forma anônima** antes de ler ou escrever — senão as regras
vão barrar a leitura/escrita e parecerá que "o Firebase não está funcionando".

> Habilite em: Console do Firebase → Authentication → Sign-in method → **Anonymous** → Ativar.
> (Gratuito, sem necessidade de senha ou e-mail do usuário.)

## 1. Inicializar e autenticar (uma vez, no início da página)

```html
<script type="module">
  import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js";
  import { getAuth, signInAnonymously } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";
  import { getDatabase, ref, push, onChildChanged, onValue } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-database.js";

  const firebaseConfig = {
    // mesmos valores do seu projeto (podem ser copiados do console ou do google-services.json)
    apiKey: "SUA_API_KEY_WEB",
    authDomain: "projetojarvis-d2dfa.firebaseapp.com",
    databaseURL: "https://projetojarvis-d2dfa-default-rtdb.firebaseio.com/",
    projectId: "projetojarvis-d2dfa"
  };

  const app = initializeApp(firebaseConfig);
  const auth = getAuth(app);
  const db = getDatabase(app);

  signInAnonymously(auth).catch((e) => console.error("Falha ao autenticar:", e));
</script>
```

## 2. Enviar um comando

Formato padrão — **todo comando usa esses 5 campos**:

```js
function enviarComando(tipo, conteudo, destino) {
  const comandosRef = ref(db, "comandos");
  push(comandosRef, {
    tipo: tipo,
    conteudo: conteudo,
    origem: "pagina-web",          // identifique este dispositivo
    destino: destino,              // deviceId do celular, "grupo:nome" ou "todos"
    hora: Date.now()
  });
}
```

### Exemplos de cada comando

```js
// Câmera
enviarComando("tirar_foto_traseira", {}, "celular-ab12cd");
enviarComando("tirar_foto_frontal", {}, "celular-ab12cd");
enviarComando("tirar_foto_ambas", {}, "celular-ab12cd");

// Alarme: por horário
enviarComando("alarme", { horario: "07:30", recorrente_diario: true }, "celular-ab12cd");
// Alarme: por atraso
enviarComando("alarme", { atraso_minutos: 15 }, "celular-ab12cd");
enviarComando("cancelar_alarme", {}, "celular-ab12cd");

// Apito remoto
enviarComando("apitar", { duracao_maxima_segundos: 60 }, "celular-ab12cd");
enviarComando("parar_alarme", {}, "celular-ab12cd"); // também para o apito

// Lanterna
enviarComando("lanterna_on", {}, "celular-ab12cd");
enviarComando("lanterna_off", {}, "celular-ab12cd");

// Outros
enviarComando("notificacao", { titulo: "Oi", mensagem: "Chegou um pacote" }, "celular-ab12cd");
enviarComando("mensagem", { texto: "Teste" }, "celular-ab12cd");
enviarComando("abrir_app", { pacote: "com.whatsapp" }, "celular-ab12cd");
enviarComando("abrir_url", { url: "https://exemplo.com" }, "celular-ab12cd");
enviarComando("falar", { texto: "Bem-vindo de volta" }, "celular-ab12cd");
enviarComando("ouvir_comando", {}, "celular-ab12cd");
enviarComando("status_bateria", {}, "celular-ab12cd");
enviarComando("status_rede", {}, "celular-ab12cd");
enviarComando("modo_nao_perturbe", { ligar: true }, "celular-ab12cd");
```

## 3. Ler respostas e imagens

```js
const respostasRef = ref(db, "respostas");
onChildChanged(respostasRef, (snapshot) => {
  const resposta = snapshot.val();
  console.log("Resposta:", resposta.status, resposta.tipo);

  if (resposta.imagemBase64) {
    const img = document.createElement("img");
    img.src = "data:image/jpeg;base64," + resposta.imagemBase64;
    document.body.appendChild(img);
  }
});
```

## 4. Descobrir o `deviceId` do celular

Abra o app no celular: o ID aparece na tela principal ("ID: celular-xxxxxx"). Esse é o
valor que você usa no campo `destino`. Para enviar a todos os dispositivos cadastrados,
use `"todos"`; para um grupo, configure o mesmo valor de grupo no app e use `"grupo:<nome>"`.
