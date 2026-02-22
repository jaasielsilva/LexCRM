package br.com.lexcrm.service;

import br.com.lexcrm.model.Cliente;
import br.com.lexcrm.repository.ClienteRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClienteImportService {

    @Autowired
    private ClienteRepository clienteRepository;

    private static final Pattern NON_DIGITS = Pattern.compile("\\D+");
    private static final Pattern CEP_PATTERN = Pattern.compile("(\\d{5}-?\\d{3})");
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final int BATCH_SIZE = 1000;
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private final List<ImportStrategy> strategies = new ArrayList<>();

    public ClienteImportService() {
        this.strategies.add(new CsvImportStrategy());
        this.strategies.add(new ExcelImportStrategy());
    }

    public ClienteImportResult importFromCsv(MultipartFile file, String tenantId) throws Exception {
        return importFile(file, tenantId);
    }

    @Transactional
    public ClienteImportResult importFile(MultipartFile file, String tenantId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Arquivo maior que o limite de 10MB.");
        }
        byte[] data = file.getBytes();
        String contentType = file.getContentType();
        String extension = getExtension(file.getOriginalFilename());
        ImportStrategy strategy = resolveStrategy(contentType, extension);
        if (strategy == null) {
            throw new IllegalArgumentException("Formato não suportado. Envie arquivos CSV, XLS ou XLSX.");
        }
        return strategy.importFile(data, tenantId);
    }

    @Transactional
    public ClienteImportResult importFromPath(Path path, String tenantId) throws Exception {
        if (path == null || !Files.exists(path)) {
            throw new IllegalArgumentException("Arquivo não encontrado.");
        }
        byte[] data = Files.readAllBytes(path);
        String extension = getExtension(path.getFileName().toString());
        ImportStrategy strategy = resolveStrategy(null, extension);
        if (strategy == null) {
            throw new IllegalArgumentException("Formato não suportado para arquivo: " + path.getFileName());
        }
        return strategy.importFile(data, tenantId);
    }

    private static char detectDelimiter(String content) {
        if (content == null) return ',';
        String firstLine = content.split("\\R", 2)[0];
        int semi = firstLine.length() - firstLine.replace(";", "").length();
        int comma = firstLine.length() - firstLine.replace(",", "").length();
        return semi > comma ? ';' : ',';
    }

    private ClienteImportResult processCsvContent(String content, char delimiter, String tenantId) throws Exception {
        List<SimpleRecord> records = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT
                .withDelimiter(delimiter)
                .withFirstRecordAsHeader()
                .withIgnoreEmptyLines()
                .withTrim()
                .parse(new StringReader(content))) {
            for (CSVRecord record : parser) {
                Map<String, String> map = record.toMap();
                if (map == null || map.isEmpty()) {
                    continue;
                }
                records.add(new SimpleRecord(map, record.getRecordNumber()));
            }
        }
        return processRecords(records, tenantId);
    }

    private ClienteImportResult processRecords(List<SimpleRecord> records, String tenantId) {
        List<String> errors = new ArrayList<>();
        int created = 0;
        int skipped = 0;

        List<String> existingCpfList = clienteRepository.findAllCpfCnpj();
        Set<String> existingCpfs = new HashSet<>();
        if (existingCpfList != null) {
            for (String cpf : existingCpfList) {
                String clean = somenteDigitos(cpf);
                if (!clean.isEmpty()) {
                    existingCpfs.add(clean);
                }
            }
        }

        Set<String> cpfInFile = new HashSet<>();
        List<Cliente> batch = new ArrayList<>();

        for (SimpleRecord record : records) {
            try {
                Map<String, String> map = record.values();
                long lineNumber = record.lineNumber();

                String nome = value(map, "nome", "name");
                String cpfCnpjRaw = value(map, "cpfCnpj", "cpf", "cpf_cnpj", "cpf/cnpj", "documento");
                String email = value(map, "email", "e-mail", "mail");
                String telefoneRaw = value(map, "telefone", "telefone1", "phone", "telefone celular", "celular");
                String indicacao = value(map, "indicacao", "indicação", "origem", "fonte");
                String enderecoRaw = value(map, "endereco", "endereço", "address");

                String cpfCnpj = somenteDigitos(cpfCnpjRaw);
                if (!cpfCnpj.isEmpty()) {
                    if (!cpfCnpjValido(cpfCnpj)) {
                        errors.add("Linha " + lineNumber + ": CPF/CNPJ inválido, valor será ignorado.");
                        cpfCnpj = "";
                    }
                }
                if (!cpfCnpj.isEmpty()) {
                    if (existingCpfs.contains(cpfCnpj)) {
                        skipped++;
                        errors.add("Linha " + lineNumber + ": já existe cliente com este CPF/CNPJ.");
                        continue;
                    }
                    if (!cpfInFile.add(cpfCnpj)) {
                        skipped++;
                        errors.add("Linha " + lineNumber + ": CPF/CNPJ duplicado no arquivo, registro ignorado.");
                        continue;
                    }
                }

                String emailNorm = null;
                if (email != null && !email.isEmpty()) {
                    String e = email.trim().toLowerCase();
                    if (!e.isEmpty()) {
                        if (!emailValido(e)) {
                            errors.add("Linha " + lineNumber + ": e-mail inválido, valor será ignorado.");
                        } else {
                            emailNorm = e;
                        }
                    }
                }

                String telefone = null;
                if (telefoneRaw != null && !telefoneRaw.isEmpty()) {
                    String tel = somenteDigitos(telefoneRaw);
                    if (!tel.isEmpty()) {
                        if (!(tel.length() == 10 || tel.length() == 11)) {
                            errors.add("Linha " + lineNumber + ": telefone inválido, valor será ignorado.");
                        } else {
                            telefone = tel;
                        }
                    }
                }

                Cliente c = new Cliente();
                c.setNome(nome == null || nome.isEmpty() ? null : nome.trim());
                c.setCpfCnpj(cpfCnpj.isEmpty() ? null : cpfCnpj);
                c.setEmail(emailNorm);
                c.setTelefone(telefone);
                c.setIndicacao(indicacao == null || indicacao.isEmpty() ? null : indicacao.trim());
                EnderecoParts ep = parseEndereco(enderecoRaw);
                if (ep != null) {
                    c.setCep(ep.cep());
                    c.setLogradouro(ep.logradouro());
                    c.setNumero(ep.numero());
                    c.setComplemento(ep.complemento());
                    c.setBairro(ep.bairro());
                    c.setCidade(ep.cidade());
                    c.setUf(ep.uf());
                }
                c.setCreatedAt(LocalDateTime.now());
                c.setTenantId(tenantId);
                batch.add(c);
                created++;

                if (batch.size() >= BATCH_SIZE) {
                    flushBatch(batch, existingCpfs);
                }
            } catch (Exception ex) {
                skipped++;
                errors.add("Linha " + record.lineNumber() + ": erro inesperado (" + ex.getMessage() + ").");
            }
        }
        flushBatch(batch, existingCpfs);
        return new ClienteImportResult(created, skipped, errors);
    }

    private void flushBatch(List<Cliente> batch, Set<String> existingCpfs) {
        if (batch.isEmpty()) {
            return;
        }
        clienteRepository.saveAll(batch);
        clienteRepository.flush();
        for (Cliente c : batch) {
            String cpf = c.getCpfCnpj();
            String clean = somenteDigitos(cpf);
            if (!clean.isEmpty()) {
                existingCpfs.add(clean);
            }
        }
        batch.clear();
    }

    private static String getExtension(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot + 1 >= name.length()) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase();
    }

    private ImportStrategy resolveStrategy(String contentType, String extension) {
        for (ImportStrategy s : strategies) {
            if (s.supports(contentType, extension)) {
                return s;
            }
        }
        return null;
    }

    private static String normalizeHeaderKey(String key) {
        if (key == null) return "";
        String n = Normalizer.normalize(key, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}", "");
        n = n.toLowerCase();
        n = n.replaceAll("[^a-z0-9]", "");
        return n;
    }

    private static String value(CSVRecord record, String... headers) {
        if (record == null) return null;
        Map<String, String> map = record.toMap();
        return value(map, headers);
    }

    private static String value(Map<String, String> map, String... headers) {
        if (map == null || map.isEmpty() || headers == null || headers.length == 0) return null;
        String[] normalizedTargets = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            normalizedTargets[i] = normalizeHeaderKey(headers[i]);
        }
        for (Map.Entry<String, String> e : map.entrySet()) {
            String normKey = normalizeHeaderKey(e.getKey());
            for (String target : normalizedTargets) {
                if (!target.isEmpty() && target.equals(normKey)) {
                    String v = e.getValue();
                    return v != null ? v.trim() : null;
                }
            }
        }
        return null;
    }

    private static String somenteDigitos(String s) {
        if (s == null) return "";
        return NON_DIGITS.matcher(s).replaceAll("");
    }

    private static boolean cpfCnpjValido(String s) {
        if (s == null) return false;
        if (s.length() == 11) return cpfValido(s);
        if (s.length() == 14) return cnpjValido(s);
        return false;
    }

    private static boolean cpfValido(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false;
        int d1 = 0;
        int d2 = 0;
        for (int i = 0; i < 9; i++) {
            int dig = cpf.charAt(i) - '0';
            d1 += dig * (10 - i);
            d2 += dig * (11 - i);
        }
        d1 = 11 - (d1 % 11);
        if (d1 >= 10) d1 = 0;
        d2 += d1 * 2;
        d2 = 11 - (d2 % 11);
        if (d2 >= 10) d2 = 0;
        return d1 == (cpf.charAt(9) - '0') && d2 == (cpf.charAt(10) - '0');
    }

    private static boolean cnpjValido(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) return false;
        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int s1 = 0;
        for (int i = 0; i < 12; i++) {
            s1 += (cnpj.charAt(i) - '0') * w1[i];
        }
        int r1 = s1 % 11;
        int d1 = r1 < 2 ? 0 : 11 - r1;
        int s2 = 0;
        for (int i = 0; i < 13; i++) {
            int val = (i < 12) ? (cnpj.charAt(i) - '0') : d1;
            s2 += val * w2[i];
        }
        int r2 = s2 % 11;
        int d2 = r2 < 2 ? 0 : 11 - r2;
        return d1 == (cnpj.charAt(12) - '0') && d2 == (cnpj.charAt(13) - '0');
    }

    private static boolean emailValido(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private static String cellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return DATA_FORMATTER.formatCellValue(cell);
    }

    private static EnderecoParts parseEndereco(String raw) {
        if (raw == null) return null;
        String original = raw.trim();
        if (original.isEmpty()) return null;

        String cep = null;
        String remaining = original;
        Matcher m = CEP_PATTERN.matcher(remaining);
        if (m.find()) {
            String cepMatch = m.group(1);
            cep = somenteDigitos(cepMatch);
            remaining = (remaining.substring(0, m.start()) + " " + remaining.substring(m.end())).replace("  ", " ").trim();
        }

        String uf = null;
        String cidade = null;
        String beforeUf = remaining;
        int slashIdx = remaining.lastIndexOf('/');
        if (slashIdx >= 0 && slashIdx + 1 < remaining.length()) {
            String ufPart = remaining.substring(slashIdx + 1).trim();
            if (!ufPart.isEmpty()) {
                if (ufPart.length() > 2) {
                    ufPart = ufPart.substring(ufPart.length() - 2);
                }
                uf = ufPart.toUpperCase();
            }
            beforeUf = remaining.substring(0, slashIdx).trim();
        }
        if (!beforeUf.isEmpty()) {
            int lastComma = beforeUf.lastIndexOf(',');
            if (lastComma >= 0 && lastComma + 1 < beforeUf.length()) {
                cidade = beforeUf.substring(lastComma + 1).trim();
            } else {
                cidade = beforeUf.trim();
            }
        }

        String logradouro = null;
        String numero = null;
        String bairro = null;
        String[] parts = original.split(",");
        if (parts.length >= 1) {
            logradouro = parts[0].trim();
        }
        if (parts.length >= 2) {
            numero = parts[1].trim();
        }
        if (parts.length >= 3) {
            bairro = parts[2].trim();
        }

        if (logradouro == null && cidade == null && cep == null) {
            return null;
        }

        return new EnderecoParts(
                (cep != null && !cep.isEmpty()) ? cep : null,
                (logradouro != null && !logradouro.isEmpty()) ? logradouro : null,
                (numero != null && !numero.isEmpty()) ? numero : null,
                null,
                (bairro != null && !bairro.isEmpty()) ? bairro : null,
                (cidade != null && !cidade.isEmpty()) ? cidade : null,
                (uf != null && !uf.isEmpty()) ? uf : null
        );
    }

    public record ClienteImportResult(int createdCount, int skippedCount, List<String> errors) {
        public int errorCount() {
            return errors != null ? errors.size() : 0;
        }
    }

    private record EnderecoParts(String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String uf) {
    }

    private record SimpleRecord(Map<String, String> values, long lineNumber) {
    }

    private interface ImportStrategy {
        boolean supports(String contentType, String extension);

        ClienteImportResult importFile(byte[] data, String tenantId) throws Exception;
    }

    private class CsvImportStrategy implements ImportStrategy {
        @Override
        public boolean supports(String contentType, String extension) {
            String ext = extension != null ? extension.toLowerCase() : "";
            if ("csv".equals(ext)) {
                return true;
            }
            if (contentType == null) {
                return false;
            }
            String ct = contentType.toLowerCase();
            return ct.contains("text/csv");
        }

        @Override
        public ClienteImportResult importFile(byte[] data, String tenantId) throws Exception {
            List<Charset> charsets = List.of(StandardCharsets.UTF_8, Charset.forName("windows-1252"), StandardCharsets.ISO_8859_1);
            Exception last = null;
            for (Charset cs : charsets) {
                try {
                    String content = new String(data, cs);
                    char delim = detectDelimiter(content);
                    return processCsvContent(content, delim, tenantId);
                } catch (Exception ex) {
                    last = ex;
                }
            }
            if (last != null) throw last;
            throw new IllegalArgumentException("Não foi possível ler o arquivo CSV.");
        }
    }

    private class ExcelImportStrategy implements ImportStrategy {
        @Override
        public boolean supports(String contentType, String extension) {
            String ext = extension != null ? extension.toLowerCase() : "";
            if ("xlsx".equals(ext) || "xls".equals(ext)) {
                return true;
            }
            if (contentType == null) {
                return false;
            }
            String ct = contentType.toLowerCase();
            return ct.contains("application/vnd.ms-excel") || ct.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }

        @Override
        public ClienteImportResult importFile(byte[] data, String tenantId) throws Exception {
            try (ByteArrayInputStream in = new ByteArrayInputStream(data);
                 Workbook workbook = WorkbookFactory.create(in)) {
                if (workbook.getNumberOfSheets() == 0) {
                    throw new IllegalArgumentException("Planilha vazia.");
                }
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    throw new IllegalArgumentException("Planilha vazia.");
                }
                int firstRow = sheet.getFirstRowNum();
                Row headerRow = sheet.getRow(firstRow);
                if (headerRow == null) {
                    throw new IllegalArgumentException("Cabeçalho não encontrado na primeira linha da planilha.");
                }
                int lastCell = headerRow.getLastCellNum();
                List<String> headers = new ArrayList<>();
                for (int c = 0; c < lastCell; c++) {
                    Cell cell = headerRow.getCell(c);
                    headers.add(cell != null ? cellValue(cell) : "");
                }
                List<SimpleRecord> records = new ArrayList<>();
                int firstDataRow = firstRow + 1;
                int lastRow = sheet.getLastRowNum();
                for (int r = firstDataRow; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    Map<String, String> map = new java.util.LinkedHashMap<>();
                    boolean allBlank = true;
                    for (int c = 0; c < headers.size(); c++) {
                        Cell cell = row.getCell(c);
                        String value = cell != null ? cellValue(cell) : "";
                        if (value != null && !value.trim().isEmpty()) {
                            allBlank = false;
                        }
                        map.put(headers.get(c), value);
                    }
                    if (allBlank) {
                        continue;
                    }
                    long lineNumber = r + 1L;
                    records.add(new SimpleRecord(map, lineNumber));
                }
                return processRecords(records, tenantId);
            }
        }
    }
}
