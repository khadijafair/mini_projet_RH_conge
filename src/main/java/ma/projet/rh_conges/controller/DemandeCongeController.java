package ma.projet.rh_conges.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.entity.*;
import ma.projet.rh_conges.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/demandes")
@RequiredArgsConstructor
public class DemandeCongeController {

    private final DemandeCongeService demandeCongeService;
    private final EmployeService employeService;
    private final TypeCongeService typeCongeService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String departement,
            @RequestParam(required = false) Long typeCongeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            Model model) {

        StatutDemande statutEnum = null;
        if (statut != null && !statut.isEmpty()) {
            try { statutEnum = StatutDemande.valueOf(statut); } catch (Exception ignored) {}
        }

        model.addAttribute("demandes",
                demandeCongeService.findWithFilters(statutEnum, departement, typeCongeId, dateDebut, dateFin));
        model.addAttribute("statuts", StatutDemande.values());
        model.addAttribute("departements", employeService.findAllDepartements());
        model.addAttribute("types", typeCongeService.findAll());
        model.addAttribute("selectedStatut", statut);
        model.addAttribute("selectedDept", departement);
        model.addAttribute("selectedType", typeCongeId);
        model.addAttribute("selectedDebut", dateDebut);
        model.addAttribute("selectedFin", dateFin);
        return "demande/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("demande", new DemandeConge());
        model.addAttribute("employes", employeService.findAll());
        model.addAttribute("types", typeCongeService.findAll());
        return "demande/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("demande") DemandeConge demande,
                       BindingResult result,
                       Model model,
                       RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("employes", employeService.findAll());
            model.addAttribute("types", typeCongeService.findAll());
            return "demande/form";
        }
        if (demande.getDateFin().isBefore(demande.getDateDebut())) {
            result.rejectValue("dateFin", "error.dateFin", "La date de fin doit être après la date de début");
            model.addAttribute("employes", employeService.findAll());
            model.addAttribute("types", typeCongeService.findAll());
            return "demande/form";
        }
        demandeCongeService.soumettreDemande(demande);
        ra.addFlashAttribute("successMsg", "Demande soumise avec succès !");
        return "redirect:/demandes";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("demande", demandeCongeService.findById(id));
        model.addAttribute("employes", employeService.findAll());
        model.addAttribute("types", typeCongeService.findAll());
        return "demande/form";
    }

    @GetMapping("/valider/{id}")
    public String valider(@PathVariable Long id, RedirectAttributes ra) {
        demandeCongeService.valider(id);
        ra.addFlashAttribute("successMsg", "Demande validée ✔");
        return "redirect:/demandes";
    }

    @GetMapping("/rejeter/{id}")
    public String rejeter(@PathVariable Long id, RedirectAttributes ra) {
        demandeCongeService.rejeter(id);
        ra.addFlashAttribute("errorMsg", "Demande rejetée ✖");
        return "redirect:/demandes";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        demandeCongeService.deleteById(id);
        ra.addFlashAttribute("successMsg", "Demande supprimée.");
        return "redirect:/demandes";
    }

    @GetMapping("/quota/{employeId}/{typeCongeId}")
    public String quota(@PathVariable Long employeId, @PathVariable Long typeCongeId, Model model) {
        int annee = LocalDate.now().getYear();
        int consommes = demandeCongeService.getJoursConsommes(employeId, typeCongeId, annee);
        TypeConge type = typeCongeService.findById(typeCongeId);
        Employe employe = employeService.findById(employeId);
        model.addAttribute("employe", employe);
        model.addAttribute("type", type);
        model.addAttribute("consommes", consommes);
        model.addAttribute("restants", Math.max(0, type.getQuotaAnnuel() - consommes));
        model.addAttribute("annee", annee);
        return "demande/quota";
    }
}
