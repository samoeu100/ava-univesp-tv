# AVA Univesp TV

Aplicativo WebView para acessar o AVA da Univesp em aparelhos Android TV e
Google TV usando apenas o controle remoto.

> Projeto independente, não oficial e ainda em fase de testes. Não possui
> vínculo com a Univesp.

## Download

- [Baixar APK 1.0.7 (recomendado)](https://github.com/samoeu100/ava-univesp-tv/raw/refs/heads/main/docs/AVA-Univesp-TV-1.0.7.apk)
- [Abrir o site do projeto](https://samoeu100.github.io/ava-univesp-tv/)
- [Baixar o código-fonte em ZIP](https://github.com/samoeu100/ava-univesp-tv/archive/refs/heads/main.zip)

## Autor

[Samuel Alexandre no LinkedIn](https://www.linkedin.com/in/samuel-alexandre-56251a145/)

## Por que o projeto foi criado

Eu faço minhas aulas pelo computador, mas queria estudar na televisão como
quem maratona uma série. Como o AVA não possui um aplicativo próprio para TV,
criei este APK para abrir o site em uma WebView adaptada ao controle remoto.

## Como funciona

O aplicativo abre o endereço oficial do AVA e oferece dois modos de navegação:

- **modo mouse:** as setas movem um cursor virtual e o botão `ok` clica;
- **modo controle:** as setas passam pelos elementos da página como em outros
  aplicativos de TV.

Na primeira abertura, o aplicativo pergunta qual modo deve ser usado. A escolha
fica salva no aparelho. Para mudar depois, basta manter o botão `ok` pressionado.

## Recursos

- acesso direto ao AVA;
- escolha entre modo mouse e modo controle;
- cursor com movimentos curtos para melhorar a precisão;
- cookies e sessão persistentes no aparelho;
- reprodução de vídeos e suporte a tela cheia;
- botão voltar integrado ao histórico da WebView;
- downloads pelo gerenciador do Android;
- conexões externas limitadas a HTTPS;
- sem banco de dados, anúncios ou rastreamento.

## Instalação direta na TV

1. Abra a página do projeto na televisão ou envie o APK para ela.
2. Nas configurações da TV, permita a instalação de aplicativos desconhecidos
   para o navegador ou gerenciador de arquivos utilizado.
3. Abra `AVA-Univesp-TV-1.0.7.apk` e selecione **instalar**.
4. Na primeira abertura, escolha **modo mouse** ou **modo controle**.

O ADB não é necessário para executar o aplicativo. Ele é apenas uma alternativa
para instalar ou consultar erros.

## Instalação com ADB

Com a depuração habilitada e a TV conectada à mesma rede do computador:

```powershell
.\adb.exe connect IP_DA_TV:5555
.\adb.exe install -r ".\AVA-Univesp-TV-1.0.7.apk"
```

Se aparecer `INSTALL_FAILED_VERSION_DOWNGRADE`, instale uma versão com número
maior ou remova a versão anterior antes de tentar novamente.

## Compilar

O projeto pode ser aberto no Android Studio. Depois da sincronização, selecione
**Build > Build APK(s)**. Também existe uma ação no GitHub que valida a
compilação automaticamente a cada alteração do código.

Configuração atual:

- pacote: `br.com.samuel.avaunivesptv`;
- versão: `1.0.7` (`versionCode 8`);
- Android mínimo: API 23;
- Android de destino: API 35;
- orientação: paisagem;
- linguagem: Java.

## Estrutura

```text
app/                         código-fonte do aplicativo
docs/                        página pública e APK para download
.github/workflows/pages.yml  publicação automática no GitHub Pages
.github/workflows/android.yml validação automática do APK
PRIVACY.md                   política de privacidade
```

## Privacidade

O aplicativo não possui servidor ou banco de dados próprio. O login acontece
diretamente no site oficial dentro da WebView. Consulte [PRIVACY.md](PRIVACY.md)
para mais informações.

## Licença

Código disponibilizado sob a licença MIT.
