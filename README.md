# Streaming Hub TV

Central de entretenimento para Android TV, com catálogo TMDB, atalhos para serviços instalados e navegação otimizada para controle remoto.

## Recursos
- Catálogo de filmes e séries com dados do TMDB em português.
- Busca de títulos.
- Minha Lista com persistência local entre sessões.
- Cards de conteúdo e serviços em proporção widescreen 16:9.
- Abertura animada da identidade Streaming Hub.
- Atalhos para abrir os aplicativos de streaming instalados.

## Compilação
O projeto usa Android Gradle Plugin 8.6.1, Kotlin 2.0.20 e Java 17. Para compilar:

```bash
gradle assembleDebug
```

A variável de ambiente `TMDB_READ_ACCESS_TOKEN` fornece o token de leitura da API do TMDB durante a compilação. **Atenção:** qualquer segredo incluído em um APK pode ser extraído; não use credenciais privilegiadas nesse campo. Para produção, prefira um serviço intermediário com controle de acesso e limites de uso.

## Observação
Este aplicativo utiliza a API do TMDB, mas não é endossado nem certificado pelo TMDB. A reprodução de conteúdo ocorre nos aplicativos oficiais instalados; o Streaming Hub não hospeda nem fornece streams.
