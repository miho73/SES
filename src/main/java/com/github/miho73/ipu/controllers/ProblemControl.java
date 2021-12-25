package com.github.miho73.ipu.controllers;

import com.github.miho73.ipu.domain.Problem;
import com.github.miho73.ipu.library.ipuac.Renderer;
import com.github.miho73.ipu.library.security.SHA;
import com.github.miho73.ipu.repositories.UserRepository;
import com.github.miho73.ipu.services.ProblemService;
import com.github.miho73.ipu.services.ResourceService;
import com.github.miho73.ipu.services.SessionService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

@Controller("ProblemControl")
@RequestMapping("/problem")
public class ProblemControl {
    private final ProblemService problemService;
    private final SessionService sessionService;
    private final UserRepository userRepository;
    private final ResourceService resourceService;
    private final Renderer renderer = new Renderer();
    private final SHA sha = new SHA();

    private int NUMBER_OF_PROBLEMS;

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @PostConstruct
    public void initProblemCount() throws SQLException {
        NUMBER_OF_PROBLEMS = problemService.getNumberOfProblems();
        LOGGER.debug("Problem count initialized to "+NUMBER_OF_PROBLEMS);
    }

    @Autowired
    public ProblemControl(ProblemService problemService, SessionService sessionService, UserRepository userRepository, ResourceService resourceService) {
        this.problemService = problemService;
        this.sessionService = sessionService;
        this.userRepository = userRepository;
        this.resourceService = resourceService;
    }

    @GetMapping("")
    public String getProblemListPage(@RequestParam(value = "page", required = false, defaultValue = "0") String pagex, Model model, HttpSession session, HttpServletResponse response) throws IOException, SQLException {
        sessionService.loadSessionToModel(session, model);
        int page = Integer.parseInt(pagex);
        int PROBLEM_PER_PAGE = 30;
        int frm = page* PROBLEM_PER_PAGE +1;
        if(frm<=0) {
            response.sendError(400);
            return null;
        }
        JSONArray list = problemService.getProblemList(frm, PROBLEM_PER_PAGE);
        JSONArray processedList = problemService.processTagsToHtml(list);
        model.addAttribute("pList", processedList.toList());
        model.addAttribute("nothing", processedList.length()==0);
        model.addAttribute("hasPrev", page != 0);
        model.addAttribute("hasNext", (NUMBER_OF_PROBLEMS-(page+1)*PROBLEM_PER_PAGE)>0);
        model.addAttribute("page", page);
        return "problem/problemList";
    }
    @GetMapping("/category")
    public String getProblemCategoryPage(@RequestParam(value = "page", required = false, defaultValue = "0") String page, Model model, HttpSession session) throws SQLException {
        sessionService.loadSessionToModel(session, model);
        Hashtable<Problem.PROBLEM_CATEGORY, Integer> dat = problemService.getNumberOfProblemsInCategory();
        model.addAllAttributes(Map.of(
                "alge", dat.get(Problem.PROBLEM_CATEGORY.ALGEBRA),
                "biol", dat.get(Problem.PROBLEM_CATEGORY.BIOLOGY),
                "comb", dat.get(Problem.PROBLEM_CATEGORY.COMBINATORICS),
                "chem", dat.get(Problem.PROBLEM_CATEGORY.CHEMISTRY),
                "numb", dat.get(Problem.PROBLEM_CATEGORY.NUMBER_THEORY),
                "phys", dat.get(Problem.PROBLEM_CATEGORY.PHYSICS),
                "geom", dat.get(Problem.PROBLEM_CATEGORY.GEOMETRY),
                "eart", dat.get(Problem.PROBLEM_CATEGORY.EARTH_SCIENCE)
        ));
        return "problem/problemCategory";
    }
    @GetMapping("/search")
    public String searchBySearch(@RequestParam(value = "page", required = false, defaultValue = "0") String page,
                                 @RequestParam(value = "cont", required = false, defaultValue = "") String contains,
                                 @RequestParam(value = "diff", required = false, defaultValue = "") String difficulty,
                                 @RequestParam(value = "cate", required = false, defaultValue = "") String category,
                                 HttpSession session, Model model) throws SQLException {
        sessionService.loadSessionToModel(session, model);
        int pg = Integer.parseInt(page);
        model.addAttribute("page", pg);
        JSONArray sResult = problemService.searchProblem(pg, contains, category, difficulty);
        JSONArray processedResult = problemService.processTagsToHtml(sResult);
        model.addAttribute("pList", processedResult.toList());
        model.addAttribute("query", contains);
        model.addAttribute("nothing", processedResult.length()==0);
        return "problem/problemSearch";
    }
    Random rand = new Random();
    @GetMapping("/random")
    public void getRandomProblem(Model model, HttpSession session, HttpServletResponse response) throws IOException, SQLException {
        sessionService.loadSessionToModel(session, model);

         rand.setSeed(System.nanoTime());
        response.sendRedirect("/problem/"+(rand.nextInt(problemService.getNumberOfProblems())+1));
    }

    @GetMapping("/{pCode}")
    public String getProblem(@PathVariable("pCode") String code, Model model, HttpSession session, HttpServletResponse response) throws IOException {
        try {
            Problem problem = problemService.getProblem(Integer.parseInt(code));
            if(problem == null) {
                response.sendError(404);
                return null;
            }
            sessionService.loadSessionToModel(session, model);
            model.addAllAttributes(Map.of(
                    "pCode", problem.getCode(),
                    "pName", problem.getName(),
                    "active", problem.isActive(),
                    "problem_ipuac", renderer.IPUACtoHTML(problem.getContent()),
                    "solution_ipuac", renderer.IPUACtoHTML(problem.getSolution())
            ));
            return "problem/problemPage";
        }
        catch (Exception e) {
            response.sendError(404);
            LOGGER.error("Cannot open problem '" +code+"'", e);
            return null;
        }
    }

    @GetMapping("/make")
    public String problemMake(Model model, HttpSession session, HttpServletResponse response) throws IOException {
        sessionService.loadSessionToModel(session, model);
        if(sessionService.hasPrivilege(SessionService.PRIVILEGES.PROBLEM_MAKE, session)) {
            response.sendError(404);
            return null;
        }
        return "problem/mkProblem";
    }
    @PostMapping(value = "/make/upload", produces = "text/plain; charset=utf-8")
    @ResponseBody
    public String imageUpload(@RequestParam(value = "img")MultipartFile resource, HttpSession session) throws IOException, NoSuchAlgorithmException, SQLException {
        if(resource.getSize() > 5000000) {
            return "size";
        }
        byte[] res = resource.getBytes();
        String hash = sha.MD5(res);
        resourceService.addResource(res, hash, sessionService.getId(session));
        return hash;
    }
    @GetMapping(value = "/lib/{src}", produces = "application/octet-stream; charset=utf-8")
    @ResponseBody
    public byte[] resourceRequest(HttpServletResponse response, @PathVariable("src")String hash) throws SQLException, IOException {
        byte[] resource = resourceService.getResource(hash);
        if(resource == null) {
            response.sendError(404);
            return null;
        }
        return resource;
    }
    @PostMapping("/register")
    public String problemRegister(HttpServletRequest request, Model model, HttpSession session, HttpServletResponse response) throws SQLException, IOException {
        if(sessionService.hasPrivilege(SessionService.PRIVILEGES.PROBLEM_MAKE, session)) {
            response.sendError(404);
            return null;
        }
        Problem problem = new Problem();
        problem.setName        (request.getParameter("name"));
        problem.setCategory    (request.getParameter("cate"));
        problem.setDifficulty  (request.getParameter("diff"));
        problem.setContent     (request.getParameter("cont"));
        problem.setSolution    (request.getParameter("solu"));
        problem.setTags        (request.getParameter("tags"));
        problem.setActive      (Boolean.parseBoolean(request.getParameter("active")));
        problem.setAuthor_name (sessionService.getName(session));
        problemService.registerProblem(problem, session);
        NUMBER_OF_PROBLEMS++;
        LOGGER.debug("Problem registered. Problem count now set to "+NUMBER_OF_PROBLEMS);
        return "redirect:/problem";
    }

    @GetMapping("/edit/{pCode}")
    public String editProblem(@PathVariable("pCode") String code, Model model, HttpSession session, HttpServletResponse response) throws IOException {
        sessionService.loadSessionToModel(session, model);
        if(sessionService.hasPrivilege(SessionService.PRIVILEGES.PROBLEM_MAKE, session)) {
            response.sendError(404);
            return null;
        }
        model.addAttribute("prob_code", code);
        return "problem/editProblem";
    }

    @PostMapping(value = "/api/get-detail", produces = "application/json; charset=utf-8")
    @ResponseBody
    public String problemDetail(HttpServletRequest request, HttpServletResponse response) throws SQLException {
        int code = Integer.parseInt(request.getParameter("code"));
        JSONObject detail = new JSONObject();
        Problem problem = problemService.getFullProblem(code);
        detail.put("cate", problem.getCategoryCode());
        detail.put("diff", problem.getDifficultyCode());
        detail.put("prob_cont", problem.getContent());
        detail.put("prob_exp", problem.getSolution());
        detail.put("prob_name", problem.getName());
        detail.put("tags", problem.getTags());
        detail.put("active", problem.isActive());
        return detail.toString();
    }

    @PostMapping("/update")
    @ResponseBody
    public String problemUpdate(HttpServletRequest request, Model model, HttpSession session, HttpServletResponse response) throws SQLException, IOException {
        if(sessionService.hasPrivilege(SessionService.PRIVILEGES.PROBLEM_MAKE, session)) {
            response.sendError(404);
            return null;
        }
        Problem problem = new Problem();
        problem.setCode        (Integer.parseInt(request.getParameter("code")));
        problem.setName        (request.getParameter("name"));
        problem.setCategory    (request.getParameter("cate"));
        problem.setDifficulty  (request.getParameter("diff"));
        problem.setContent     (request.getParameter("cont"));
        problem.setSolution    (request.getParameter("solu"));
        problem.setTags        (request.getParameter("tags"));
        problem.setActive      (Boolean.parseBoolean(request.getParameter("active")));
        problemService.updateProblem(problem);
        return "/problem/"+problem.getCode();
    }

    @PostMapping("/api/solrep")
    @ResponseBody
    public String registerSolve(HttpServletRequest request, HttpSession session, HttpServletResponse response) throws SQLException {
        Connection connection = userRepository.openConnectionForEdit();
        try {
            if(!sessionService.checkLogin(session) || sessionService.hasPrivilege(SessionService.PRIVILEGES.USER, session)) {
                response.setStatus(403);
                return "forb";
            }

            String uid = sessionService.getId(session);
            Timestamp last_submit = (Timestamp) userRepository.getUserDataById(uid, "last_solve", connection);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            if(last_submit != null ){
                if((now.getTime()-last_submit.getTime())/1000 < 60) {
                    response.setStatus(429);
                    return "time";
                }
            }
            userRepository.updateUserTSById(uid, "last_solve", now, connection);

            int code = Integer.parseInt(request.getParameter("code"));
            int time = Integer.parseInt(request.getParameter("time"));
            boolean result = request.getParameter("res").equals("1");
            int userCode = sessionService.getCode(session);
            problemService.registerSolution(code, time, result, userCode, connection);
            userRepository.commit(connection);
            return "ok";
        }
        catch (Exception e) {
            userRepository.rollback(connection);
            LOGGER.error("Cannot register solution", e);
            response.setStatus(500);
            return "error";
        }
        finally {
            userRepository.close(connection);
        }
    }
}
