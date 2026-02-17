package br.com.lexcrm.service;

import br.com.lexcrm.model.Documento;
import br.com.lexcrm.model.EtapaProcesso;
import br.com.lexcrm.model.Processo;
import br.com.lexcrm.repository.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Service
public class FileStorageService {

    @Autowired
    private DocumentoRepository documentoRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final String[] EXTENSOES_PERMITIDAS = {
            ".pdf", ".jpg", ".jpeg", ".png", ".docx", ".xlsx", ".doc", ".xls"
    };

    private static final long TAMANHO_MAXIMO = 50 * 1024 * 1024; // 50MB

    public Documento salvarArquivo(MultipartFile file, Processo processo,
            EtapaProcesso etapa, String tipo) throws IOException {
        // 1. Validar arquivo
        validarArquivo(file);

        // 2. Gerar nome único
        String extensao = getExtensao(file.getOriginalFilename());
        String nomeArquivo = UUID.randomUUID().toString() + extensao;

        // 3. Construir caminho
        String caminho = construirCaminho(processo, tipo);

        // 4. Criar diretórios se não existirem
        Path diretorioCompleto = Paths.get(uploadDir, caminho);
        Files.createDirectories(diretorioCompleto);

        // 5. Salvar arquivo
        Path arquivoDestino = diretorioCompleto.resolve(nomeArquivo);
        file.transferTo(arquivoDestino.toFile());

        // 6. Criar registro no banco
        Documento doc = new Documento();
        doc.setNomeOriginal(file.getOriginalFilename());
        doc.setNomeArquivo(nomeArquivo);
        doc.setCaminho(caminho + "/" + nomeArquivo);
        doc.setTipo(tipo);
        doc.setTamanho(file.getSize());
        doc.setMimeType(file.getContentType());
        doc.setDataUpload(LocalDateTime.now());
        doc.setProcesso(processo);
        doc.setEtapa(etapa);
        doc.setTenantId(processo.getTenantId());
        doc.setUploadedBy(getCurrentUsername());

        return documentoRepository.save(doc);
    }

    private void validarArquivo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }

        // Validar tamanho
        if (file.getSize() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("Arquivo muito grande. Máximo: 50MB");
        }

        // Validar extensão
        String nomeOriginal = file.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.isEmpty()) {
            throw new IllegalArgumentException("Nome de arquivo inválido");
        }

        String nomeMinusculo = nomeOriginal.toLowerCase();
        boolean extensaoValida = Arrays.stream(EXTENSOES_PERMITIDAS)
                .anyMatch(nomeMinusculo::endsWith);

        if (!extensaoValida) {
            throw new IllegalArgumentException(
                    "Tipo de arquivo não permitido. Permitidos: " +
                            String.join(", ", EXTENSOES_PERMITIDAS));
        }
    }

    private String getExtensao(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.contains(".")) {
            return "";
        }
        return nomeArquivo.substring(nomeArquivo.lastIndexOf("."));
    }

    private String construirCaminho(Processo processo, String tipo) {
        // uploads/{tenantId}/{processoId}/{tipo}/
        return String.format("%s/%d/%s",
                processo.getTenantId(),
                processo.getId(),
                tipo.toLowerCase());
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    public Path getArquivoPath(Documento documento) {
        return Paths.get(uploadDir, documento.getCaminho());
    }
}
