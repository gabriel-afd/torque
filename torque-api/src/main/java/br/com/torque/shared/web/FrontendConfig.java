package br.com.torque.shared.web;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serve o build do Angular empacotado em {@code classpath:/static/} (deploy em imagem única).
 *
 * <p>Arquivo existente é servido direto. Rota do Angular ({@code /quadro}, {@code /ordens/17}) recebe o
 * {@code index.html}, e o roteador do frontend cuida dela. Caminhos sob {@code /api} e arquivos inexistentes
 * (com extensão) não recebem o {@code index.html}: seguem para o 404 padrão da API.
 *
 * <p>Substitui o mapeamento estático padrão do Spring Boot ({@code spring.web.resources.add-mappings: false}).
 */
@Configuration
public class FrontendConfig implements WebMvcConfigurer {

    private static final String INDEX = "index.html";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new RotasDoAngular());
    }

    static class RotasDoAngular extends PathResourceResolver {

        @Override
        protected Resource getResource(String caminho, Resource local) throws IOException {
            Resource arquivo = super.getResource(caminho, local);
            if (arquivo != null) {
                return arquivo;
            }
            return ehRotaDoAngular(caminho) ? super.getResource(INDEX, local) : null;
        }

        static boolean ehRotaDoAngular(String caminho) {
            if (caminho.equals("api") || caminho.startsWith("api/")) {
                return false;
            }
            String ultimoTrecho = caminho.substring(caminho.lastIndexOf('/') + 1);
            return !ultimoTrecho.contains(".");
        }
    }
}
