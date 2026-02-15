package br.com.lexcrm.controller;

import br.com.lexcrm.model.*;
import br.com.lexcrm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/processos")
public class ProcessoController {

    @Autowired
    private ProcessoRepository processoRepository;
    @Autowired
    private EtapaProcessoRepository etapaProcessoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private DocumentoChecklistRepository documentoChecklistRepository;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("processos", processoRepository.findAll());
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("advogados", usuarioRepository.findAll());
        return "processos/index";
    }

    @GetMapping("/pendentes/fragment")
    public String getProcessosPendentesFragment(Model model) {
        model.addAttribute("processos", processoRepository.findByStatus("Pendente"));
        return "processos/index :: list";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String query, Model model) {
        if (query != null && !query.isEmpty()) {
            model.addAttribute("processos", processoRepository.buscarPorTermo(query));
        } else {
            model.addAttribute("processos", processoRepository.findAll());
        }
        return "processos/index :: list";
    }

    @PostMapping("/novo")
    public String criarProcesso(Processo processo) {
        // Set defaults
        processo.setStatus("Pendente");
        processo.setDataAbertura(LocalDate.now());
        processo.setTenantId("T001"); // Fixed for now

        // Create Default Stages
        List<String> stageNames = Arrays.asList(
            "Cadastra o cliente",
            "Documentação",
            "Petição Inicial",
            "Citação",
            "Contestação",
            "Audiência",
            "Perícia",
            "Alegações Finais",
            "Sentença",
            "Recurso",
            "Trânsito em Julgado",
            "Execução"
        );

        for (int i = 0; i < stageNames.size(); i++) {
            EtapaProcesso etapa = new EtapaProcesso();
            etapa.setNome(stageNames.get(i));
            etapa.setDescricao("Etapa padrão do fluxo processual");
            etapa.setOrdem(i + 1);
            etapa.setProcesso(processo);
            etapa.setStatus("Pendente");
            
            // Add Checklist for "Documentação" stage
            if ("Documentação".equals(etapa.getNome())) {
                List<String> docs = Arrays.asList(
                    "RG ou CNH",
                    "Comprovante de residência",
                    "Carteira de trabalho (Digital/PDF)",
                    "Últimos 3 holerites antes do diagnóstico",
                    "Documentos médicos (Laudos, Tomografia, Raio-X, Prontuário)",
                    "Dados bancários para indenização",
                    "E-mail para envio de documentação",
                    "B.O ou C.A.T (caso tenha)"
                );
                
                for (String docName : docs) {
                    DocumentoChecklist doc = new DocumentoChecklist();
                    doc.setNome(docName);
                    doc.setEntregue(false);
                    doc.setEtapa(etapa);
                    etapa.getChecklist().add(doc);
                }
            }
            
            processo.getEtapas().add(etapa);
        }

        processoRepository.save(processo);
        
        return "redirect:/processos";
    }

    @GetMapping("/{id}/timeline")
    public String getTimeline(@PathVariable Long id, Model model) {
        Processo processo = processoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + id));

        // Adiciona atributo HTMX OOB para atualizar a barra de progresso
        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("hxOobSwap", true); // Flag auxiliar se necessário

        // Retorna um template wrapper que inclui os dois fragmentos
        // Como o Thymeleaf normal retorna um único template, vamos retornar um template especial 'fragments/htmx-updates'
        // Ou podemos concatenar manualmente se não quisermos criar um novo arquivo, mas criar um arquivo é mais limpo.
        // Opção B: Retornar o timeline e adicionar o progress-bar como OOB no mesmo response.
        // O Thymeleaf + HTMX geralmente requer que retornemos múltiplos fragmentos.
        // Vamos usar um fragmento wrapper simples.
        
        return "fragments/htmx-response-wrapper";
    }

    @PostMapping("/{processoId}/etapa/{etapaId}/toggle")
    public String toggleEtapa(@PathVariable Long processoId, @PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        
        // Cycle Status
        switch (etapa.getStatus()) {
            case "Pendente":
                etapa.setStatus("Em Andamento");
                etapa.setData(LocalDate.now());
                break;
            case "Em Andamento":
                etapa.setStatus("Concluído");
                etapa.setData(LocalDate.now());
                break;
            case "Concluído":
                etapa.setStatus("Pendente");
                etapa.setData(null);
                break;
            default:
                etapa.setStatus("Pendente");
        }
        
        etapaProcessoRepository.save(etapa);
        
        // Return the updated timeline for this process
        Processo processo = processoRepository.findById(processoId).orElseThrow();
        
        // Prepare response with both timeline and progress bar (OOB)
        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("hxOobSwap", true);
        
        return "fragments/htmx-response-wrapper";
    }

    @PostMapping("/{processoId}/etapa/{etapaId}/checklist/{docId}/toggle")
    @jakarta.transaction.Transactional
    public String toggleChecklist(@PathVariable Long processoId, 
                                  @PathVariable Long etapaId, 
                                  @PathVariable Long docId, 
                                  Model model) {
        
        // 1. Busca apenas a etapa (que já traz o checklist via EAGER)
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId)
                .orElseThrow(() -> new IllegalArgumentException("Etapa não encontrada"));

        // 2. Validação de Segurança: A etapa pertence ao processo informado?
        if (!etapa.getProcesso().getId().equals(processoId)) {
            throw new IllegalArgumentException("Etapa não pertence ao processo informado");
        }

        // 3. Encontra o documento DENTRO da lista da etapa (garante consistência)
        DocumentoChecklist doc = etapa.getChecklist().stream()
                .filter(d -> d.getId().equals(docId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Documento não pertence a esta etapa"));

        // 4. Atualiza o estado
        doc.setEntregue(!doc.isEntregue());

        // 5. Recalcula status da etapa
        boolean allCompleted = etapa.getChecklist().stream().allMatch(DocumentoChecklist::isEntregue);

        if (allCompleted) {
            etapa.setStatus("Concluído");
            etapa.setData(LocalDate.now());
        } else {
            // Se estava concluído mas desmarcou um item, volta para Em Andamento (ou Pendente se nenhum marcado)
            if ("Concluído".equals(etapa.getStatus())) {
                etapa.setStatus("Em Andamento");
            } else if (etapa.getChecklist().stream().noneMatch(DocumentoChecklist::isEntregue)) {
                 etapa.setStatus("Pendente");
            } else {
                 etapa.setStatus("Em Andamento");
            }
        }

        // 6. Salva apenas a etapa. O CascadeType.ALL salvará o documento automaticamente.
        etapaProcessoRepository.save(etapa);

        // Retorna o fragmento atualizado
        model.addAttribute("etapa", etapa);
        model.addAttribute("processo", etapa.getProcesso());
        model.addAttribute("hxOobSwap", true);
        
        return "fragments/checklist-modal :: content"; 
    }

    @GetMapping("/etapa/{etapaId}/checklist")
    public String getChecklistModal(@PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        model.addAttribute("etapa", etapa);
        return "fragments/checklist-modal :: content";
    }
}