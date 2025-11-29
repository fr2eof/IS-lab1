package se.ifmo.ru.back.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/index.html")
    public String indexHtml() {
        return "index";
    }

    @GetMapping("/chapters.html")
    public String chapters() {
        return "chapters";
    }

    @GetMapping("/coordinates.html")
    public String coordinates() {
        return "coordinates";
    }

    @GetMapping("/special-operations.html")
    public String specialOperations() {
        return "special-operations";
    }

    @GetMapping("/import.html")
    public String importPage() {
        return "import";
    }

    @GetMapping("/import-history.html")
    public String importHistory() {
        return "import-history";
    }
}
