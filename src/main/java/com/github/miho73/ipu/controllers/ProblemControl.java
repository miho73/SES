package com.github.miho73.ipu.controllers;

import com.github.miho73.ipu.domain.Problem;
import com.github.miho73.ipu.domain.ProblemListAPI;
import com.github.miho73.ipu.repositories.SolutionRepository;
import com.github.miho73.ipu.repositories.UserRepository;
import com.github.miho73.ipu.services.ProblemService;
import com.github.miho73.ipu.services.SessionService;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

@Controller("ProblemControl")
@RequestMapping("/problem")
public class ProblemControl {
    private final ProblemService problemService;
    private final SessionService sessionService;
    private final SolutionRepository solutionRepository;
    private final UserRepository userRepository;

    @Autowired
    public ProblemControl(ProblemService problemService, SessionService sessionService, SolutionRepository solutionRepository, UserRepository userRepository) {
        this.problemService = problemService;
        this.sessionService = sessionService;
        this.solutionRepository = solutionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("")
    public String getProblemListPage(@RequestParam(value = "page", required = false, defaultValue = "0") String page, Model model, HttpSession session) {
        sessionService.loadSessionToModel(session, model);
        model.addAttribute("page", Integer.parseInt(page));
        return "problem/problemList";
    }

    @PostMapping("/api/get")
    @ResponseBody
    public String getProblemList(@ModelAttribute ProblemListAPI pla, HttpServletResponse response) throws SQLException {
        response.setContentType("application/json");
        JSONArray list = problemService.getProblemList(pla.frm, pla.len);
        return list.toString();
    }

    @GetMapping("/{pCode}")
    public String getProblem(@PathVariable("pCode") String code, Model model, HttpSession session) throws SQLException {
        Problem problem = problemService.getProblem(Integer.parseInt(code));
        sessionService.loadSessionToModel(session, model);
        model.addAllAttributes(Map.of(
                "pCode", problem.getCode(),
                "pName", problem.getName(),
                "content", problem.getContent(),
                "solution", problem.getSolution(),
                "answer", problem.getAnswer(),
                "hint", problem.getHint(),
                "hasHint", problem.isHasHint(),
                "extrTabs", problem.getExternalTabs()
        ));
        return "problem/problemPage";
    }

    @GetMapping("/make")
    public String problemMake(Model model, HttpSession session, HttpServletResponse response) throws IOException {
        sessionService.loadSessionToModel(session, model);
        if(!sessionService.hasPrivilege(SessionService.PRIVILEGES.PROBLEM_MAKE, session)) {
            response.sendError(404);
            return null;
        }
        return "problem/mkProblem";
    }
    @PostMapping("/register")
    public String problemRegister(HttpServletRequest request, Model model, HttpSession session, HttpServletResponse response) throws SQLException, IOException {
        if(!sessionService.hasPrivilege(SessionService.PRIVILEGES.PROBLEM_MAKE, session)) {
            response.sendError(404);
            return null;
        }
        Problem problem = new Problem();
        problem.setName        (request.getParameter("name"));
        problem.setCategory    (request.getParameter("cate"));
        problem.setDifficulty  (request.getParameter("diff"));
        problem.setContent     (request.getParameter("cont"));
        problem.setSolution    (request.getParameter("solu"));
        problem.setAnswer      (request.getParameter("answ"));
        problem.setHint        (request.getParameter("hint"));
        problem.setHasHint     (request.getParameter("hasH").equals("true"));
        problem.setExternalTabs(request.getParameter("extr"));
        problem.setTags        (request.getParameter("tags"));
        problemService.registerProblem(problem, session);
        return "redirect:/problem";
    }

    @PostMapping("/api/solrep")
    @Transactional
    @ResponseBody
    //TODO: Implement transaction
    public String registerSolve(HttpServletRequest request, HttpSession session, HttpServletResponse response) throws SQLException {
        if(!sessionService.checkLogin(session) || !sessionService.hasPrivilege(SessionService.PRIVILEGES.USER, session)) {
            return "forb";
        }

        String uid = sessionService.getId(session);
        Timestamp last_submit = (Timestamp) userRepository.getUserDataById(uid, "last_solve");
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if(last_submit != null ){
             if((now.getTime()-last_submit.getTime())/1000 < 60) {
                 response.setStatus(429);
                 return "time";
             }
        }
        userRepository.updateUserTSById(uid, "last_solve", now);

        long code = Long.parseLong(request.getParameter("code"));
        long time = Long.parseLong(request.getParameter("time"));
        boolean result = request.getParameter("res").equals("1");
        long userCode = sessionService.getCode(session);

        solutionRepository.addSolution(code, time, result, userCode);
        return "ok";
    }
}