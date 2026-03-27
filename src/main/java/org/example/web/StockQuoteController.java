package org.example.web;

import org.example.service.StockQuoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stock")
public class StockQuoteController {
    private final StockQuoteService stockQuoteService;

    public StockQuoteController(StockQuoteService stockQuoteService) {
        this.stockQuoteService = stockQuoteService;
    }

    @GetMapping("/quote")
    public Map<String, Object> quote(@RequestParam("code") String code) {
        return stockQuoteService.fetchQuote(code);
    }

    @GetMapping("/series")
    public Map<String, Object> series(@RequestParam("code") String code,
                                      @RequestParam(value = "period", required = false) String period) {
        return stockQuoteService.fetchSeries(code, period);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return stockQuoteService.health();
    }
}
