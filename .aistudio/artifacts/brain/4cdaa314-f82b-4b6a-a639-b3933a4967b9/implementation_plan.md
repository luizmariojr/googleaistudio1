# Correção Definitiva do Upload e Visualização de Anexos no Supabase Storage

Correção do destino de upload e vínculo das fotos de canhotos de entrega, apontando para o bucket oficial `romaneio_anexos` no Supabase Storage, garantindo que as imagens anexadas pelo motorista fiquem acessíveis no portal web e backoffice da logística.

### User Review & Critical Decisions

> [!IMPORTANT]
> Os testes realizados diretamente contra o servidor Supabase revelaram que o bucket `canhotos` não existe (retorna erro HTTP 404 `NoSuchBucket`), enquanto o bucket oficial do sistema é **`romaneio_anexos`**.

- **Bucket Oficial Identificado**: O upload de comprovantes deve ser direcionado prioritariamente para `romaneio_anexos`.
- **Formato do Caminho do Anexo (`p_canhoto_path`)**: No banco de dados e portal, o caminho salvo deve ser o identificador relativo no Storage (ex: `romaneio_anexos/{romaneio_id}/{nome_arquivo}.jpg` ou `{romaneio_id}/{nome_arquivo}.jpg`), eliminando qualquer envio de caminhos de arquivos locais do celular (`/data/user/0/...`).
- **Resiliência Offline e Retentativa**: Se a conexão falhar no momento da captura ou upload, a confirmação continuará salva localmente e o arquivo será reenviado com token válido assim que a internet for restabelecida.

---

### 1. Overview & Core Concept

- **What It Does**: Ao confirmar a entrega (por NF ou pelo romaneio completo), a foto tirada ou selecionada pelo motorista é enviada para o bucket oficial `romaneio_anexos` do Supabase Storage. Em seguida, a função RPC do Supabase (`confirmar_entrega_nf_portal` ou `confirmar_entrega_portal`) é acionada com o caminho remoto exato, permitindo que o portal web abra e visualize o documento imediatamente.
- **Target Audience / Persona**: Motoristas e operadores de logística do aplicativo Bicicletão Logística, bem como a equipe de retaguarda/expedição que audita os comprovantes de entrega no portal web.
- **Key Value**: Elimina o erro de anexos em branco no portal web, assegura validade jurídica e operacional dos canhotos digitais e preserva o funcionamento offline com sincronização automática.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. *Captura do Comprovante*: O motorista abre o romaneio ou a NF específica, toca em "Confirmar Entrega", tira a foto do canhoto pela câmera ou galeria.
  2. *Feedback Visual*: A miniatura do canhoto é exibida com badge de status indicando "Comprovante anexado".
  3. *Confirmação*: Ao salvar, se online, o app envia a foto para `romaneio_anexos` e confirma no banco; se offline, salva a imagem no armazenamento interno do app e agenda o envio.
  4. *Visualização no Portal*: Os operadores que acessam o portal web clicam no botão "Ver Canhoto" e o arquivo real é aberto via URL pública/assinada do bucket `romaneio_anexos`.
- **Visual Identity & Theme**:
  - Mantém o padrão visual Material 3 em tons de azul escuro e verde status já presentes no app.
  - Indicadores de upload com barra de progresso suave e snackbar de confirmação clara.
- **Interactive Feedback & Motion**:
  - Alerta sutil quando o upload do arquivo é concluído com sucesso.
  - Caso o motorista não anexe foto ou o arquivo esteja corrompido, geração transparente de comprovante digital legível com dados da NF, data/hora e identificador do motorista.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Alvo do Bucket no Supabase Storage**
  - *Chosen Approach*: Apontar o upload para o bucket `romaneio_anexos`, mantendo lista de contingência.
  - *Why*: Testes reais no servidor confirmaram que o bucket ativo na instância do Supabase é `romaneio_anexos`. Buckets genéricos como `canhotos` retornam `404 NoSuchBucket`.
  - *Alternatives Considered*: Criar um novo bucket via app; descartado porque o app opera com credenciais de cliente/anon e não tem privilégios de administração DDL para criar buckets.
- **Decision 2: Formatação do parâmetro `p_canhoto_path`**
  - *Chosen Approach*: Enviar estritamente o caminho remoto do storage (ex: `{romaneioId}/{fileName}`). Caso ocorra falha temporária no upload, não gravar o caminho absoluto `/data/user/0/...` no banco remoto, mas sim reter a sincronização até o upload ser concluído.
  - *Why*: Gravar caminhos locais do celular no banco remoto inutiliza a visualização no portal web por outros usuários.
- **Decision 3: Redimensionamento e Otimização da Imagem**
  - *Chosen Approach*: Otimizar a imagem capturada para JPEG com resolução máxima de 1920x1080 e compressão de 85% antes do upload.
  - *Why*: Reduz o tráfego de dados para ~200-400KB sem perder legibilidade da assinatura e dados da nota fiscal, tornando o upload muito mais rápido em 3G/4G no trânsito.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                 App Android (Motorista)                     │
│                                                             │
│  [Camera / Galeria] ──> [Compressão JPEG / Cache Local]     │
│                                │                            │
│                                ▼                            │
│                  [DeliveryRepository (Offline)]             │
│                                │                            │
│                  (Se Online ou na Sincronização)            │
│                                │                            │
│            ┌───────────────────┴───────────────────┐        │
│            ▼                                       ▼        │
│   [Upload Binário]                        [RPC Confirmação] │
│  (Supabase Storage)                      (Supabase PostgREST)
│  Bucket: romaneio_anexos                  confirmar_entrega │
└────────────┬───────────────────────────────────────┬────────┘
             │                                       │
             ▼                                       ▼
┌─────────────────────────────────────────────────────────────┐
│                      Supabase Cloud                         │
│                                                             │
│  Storage Bucket:                                            │
│    romaneio_anexos/{romaneio_id}/{canhoto_nf_123.jpg}       │
│                                                             │
│  Tabelas / RPC:                                             │
│    p_canhoto_path = "{romaneio_id}/{canhoto_nf_123.jpg}"    │
│    romaneios / notas_fiscais status = 'entregue'            │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                  Portal Web / Backoffice                    │
│                                                             │
│  Lê URL do arquivo:                                         │
│  /storage/v1/object/public/romaneio_anexos/{caminho}        │
│  Exibe o canhoto com sucesso para a equipe de logística     │
└─────────────────────────────────────────────────────────────┘
```

- **Componentes a Modificar**:
  1. `SupabaseClient.kt`:
     - Ajustar `uploadCanhotoFileToStorage` para usar `romaneio_anexos` como bucket primário.
     - Suportar formatação padronizada do caminho relativo.
     - Aplicar headers adequados de autenticação e tipo de mídia (`image/jpeg`).
  2. `DeliveryRepository.kt`:
     - Garantir que a sincronização só avance para o RPC de confirmação quando o upload para o Storage for bem-sucedido ou quando o caminho remoto estiver garantido.
     - Evitar envio de paths locais (`/data/user/0/...`) para o RPC.
  3. `DeliveryConfirmScreen.kt`:
     - Otimizar a captura da foto, garantindo que o arquivo seja gravado em cache seguro antes de disparar o fluxo de confirmação.
