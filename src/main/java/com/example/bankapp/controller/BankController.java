package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        loadDashboard(model, keyword);
        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(@RequestParam String accountNumber,
                              @RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam BigDecimal balance,
                              RedirectAttributes redirectAttributes) {
        try {
            bankService.createCustomer(accountNumber, fullName, email, balance);
            redirectAttributes.addFlashAttribute("successMessage", "Customer added: " + fullName);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping(value = "/transfer", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String transferForm(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                               BindingResult bindingResult,
                               Model model) {
        return processTransfer(request, bindingResult, model);
    }

    @PostMapping(value = "/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String transferJson(@Valid @RequestBody TransferRequest request,
                               BindingResult bindingResult,
                               Model model) {
        return processTransfer(request, bindingResult, model);
    }

    private String processTransfer(TransferRequest request, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("transferRequest", request);
            model.addAttribute("org.springframework.validation.BindingResult.transferRequest", bindingResult);
            loadDashboard(model, null);
            return "bank";
        }

        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure());
            model.addAttribute("successMessage", "Transfer completed successfully!");
            model.addAttribute("transferRequest", new TransferRequest());
        } catch (Exception e) {
            auditService.recordFailure(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    e.getMessage());
            model.addAttribute("errorMessage",
                    "Transfer failed: " + e.getMessage() + " All changes were rolled back.");
            model.addAttribute("transferRequest", request);
        }

        loadDashboard(model, null);
        return "bank";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String history(@PathVariable String accountNumber, Model model) {
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("customer", bankService.findCustomer(accountNumber).orElse(null));
        model.addAttribute("transactions", bankService.getHistory(accountNumber));
        model.addAttribute("totalSent", bankService.getTotalSent(accountNumber));
        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transactions", bankService.getLargeTransfers(min));
        return "large-transfers";
    }

    @GetMapping("/deposit")
    public String depositPage() {
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam String accountNumber,
                          @RequestParam BigDecimal amount,
                          Model model) {
        try {
            bankService.deposit(accountNumber, amount);
            model.addAttribute("successMessage", "Deposit completed: LKR " + amount + " to " + accountNumber);
        } catch (Exception e) {
            auditService.recordFailure("CASH", accountNumber, amount, e.getMessage());
            model.addAttribute("errorMessage", "Deposit failed: " + e.getMessage());
        }
        return "deposit";
    }

    @GetMapping("/withdraw")
    public String withdrawPage() {
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String accountNumber,
                           @RequestParam BigDecimal amount,
                           Model model) {
        try {
            bankService.withdraw(accountNumber, amount);
            model.addAttribute("successMessage", "Withdrawal completed: LKR " + amount + " from " + accountNumber);
        } catch (Exception e) {
            auditService.recordFailure(accountNumber, "CASH", amount, e.getMessage());
            model.addAttribute("errorMessage", "Withdrawal failed: " + e.getMessage());
        }
        return "withdraw";
    }

    private void loadDashboard(Model model, String keyword) {
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("customers", bankService.searchCustomers(keyword));
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("customerCount", bankService.countCustomers());
        model.addAttribute("successCount", bankService.countByStatus(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankService.countByStatus(TransactionStatus.FAILED));
        model.addAttribute("transactions", bankService.getLatestTransactions());
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
    }
}