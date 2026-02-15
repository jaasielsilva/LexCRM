package br.com.lexcrm.controller;

import br.com.lexcrm.model.Usuario;
import br.com.lexcrm.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String getPerfil(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        model.addAttribute("usuario", usuario);
        return "fragments/perfil-modal :: content";
    }

    @PostMapping("/salvar")
    @ResponseBody
    public String salvarPerfil(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String nomeCompleto,
            @RequestParam String username) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        // Verifica se o username mudou e se já existe
        if (!usuario.getUsername().equals(username) && usuarioRepository.findByUsername(username).isPresent()) {
            return "<script>window.uiToastError('Este nome de usuário já está em uso.');</script>";
        }

        usuario.setNomeCompleto(nomeCompleto);
        usuario.setUsername(username);
        usuarioRepository.save(usuario);

        return "<script>window.uiToastSuccess('Perfil atualizado com sucesso! (Username alterado, recomendo re-logar se necessário)'); closeModal(); location.reload(); </script>";
    }

    @PostMapping("/senha")
    @ResponseBody
    public String trocarSenha(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String senhaAtual,
            @RequestParam String novaSenha) {
        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!passwordEncoder.matches(senhaAtual, usuario.getPassword())) {
            return "<script>window.uiToastError('A senha atual está incorreta.');</script>";
        }

        usuario.setPassword(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        return "<script>window.uiToastSuccess('Senha alterada com sucesso!'); closeModal();</script>";
    }
}
