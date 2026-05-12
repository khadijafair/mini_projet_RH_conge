package ma.projet.rh_conges.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.TypeConge;
import ma.projet.rh_conges.service.TypeCongeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/types-conge")
@RequiredArgsConstructor
public class TypeCongeController {

    private final TypeCongeService typeCongeService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("types", typeCongeService.findAll());
        return "typeconge/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("typeConge", new TypeConge());
        return "typeconge/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("typeConge") TypeConge typeConge,
                       BindingResult result,
                       RedirectAttributes ra) {
        if (result.hasErrors()) return "typeconge/form";
        typeCongeService.save(typeConge);
        ra.addFlashAttribute("successMsg", "Type de congé enregistré !");
        return "redirect:/types-conge";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("typeConge", typeCongeService.findById(id));
        return "typeconge/form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            typeCongeService.deleteById(id);
            ra.addFlashAttribute("successMsg", "Type de congé supprimé.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Impossible de supprimer ce type (des demandes lui sont liées).");
        }
        return "redirect:/types-conge";
    }
}
