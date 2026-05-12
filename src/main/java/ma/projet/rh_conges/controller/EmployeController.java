package ma.projet.rh_conges.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.Employe;
import ma.projet.rh_conges.service.EmployeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employes")
@RequiredArgsConstructor
public class EmployeController {

    private final EmployeService employeService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("employes", employeService.findAll());
        model.addAttribute("departements", employeService.findAllDepartements());
        return "employe/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("employe", new Employe());
        return "employe/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("employe") Employe employe,
                       BindingResult result,
                       RedirectAttributes ra) {
        if (result.hasErrors()) return "employe/form";
        employeService.save(employe);
        ra.addFlashAttribute("successMsg", "Employé enregistré avec succès !");
        return "redirect:/employes";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("employe", employeService.findById(id));
        return "employe/form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            employeService.deleteById(id);
            ra.addFlashAttribute("successMsg", "Employé supprimé.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Impossible de supprimer cet employé (des demandes lui sont liées).");
        }
        return "redirect:/employes";
    }
}
