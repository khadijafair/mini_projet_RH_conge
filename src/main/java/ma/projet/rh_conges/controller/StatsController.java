package ma.projet.rh_conges.controller;

import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.service.DemandeCongeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final DemandeCongeService demandeCongeService;

    @GetMapping
    public String stats(Model model) {
        model.addAttribute("statsGlobales", demandeCongeService.getStatistiquesGlobales());
        model.addAttribute("joursParDept", demandeCongeService.getJoursParDepartement());
        model.addAttribute("statsParType", demandeCongeService.getStatistiquesParType());
        return "stats/dashboard";
    }
}
