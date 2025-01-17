package com.github.miho73.ipu.services;

import com.github.miho73.ipu.domain.Problem;
import com.github.miho73.ipu.library.Converters;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.Vector;

@Service
public class TagService {
    @Autowired private Converters converters = new Converters();
    @Autowired private SessionService sessionService;
    @Autowired private UserService userService;

    public String processTagsToHtml(Problem problem) {
        JSONArray tags = new JSONArray(problem.getTags());
        StringBuilder html = new StringBuilder();
        tags.forEach((tagx)-> {
            JSONObject tag = (JSONObject) tagx;
            html.append("<span class=\"tag tag-custom\" style=\"background-color: #")
                    .append(tag.get("back"))
                    .append("; color: #")
                    .append(tag.get("color"))
                    .append(";\">")
                    .append(tag.get("content"))
                    .append("</span>");
        });
        html.append("<span class=\"tag tag-diff\" style=\"background-color: ")
                .append(converters.convertDiffColor(problem.getDifficultyCode()))
                .append(";\">")
                .append(converters.convertDiff(problem.getDifficultyCode()))
                .append("</span>");
        return html.toString();
    }

    public JSONArray processTagsToHtml(JSONArray sResult, HttpSession user) throws SQLException {
        JSONArray processedResult = new JSONArray();
        Vector<String> starList = new Vector<>();
        if(user != null && sessionService.checkLogin(user)) {
            starList = userService.getUserStaredList(sessionService.getCode(user));
        }
        Vector<String> finalStarList = starList;
        sResult.forEach(result->{
            JSONObject workingProblem = ((JSONObject)result);
            JSONArray tags = (JSONArray)workingProblem.get("tags");
            StringBuilder html = new StringBuilder();
            tags.forEach((tagx)->{
                JSONObject tag = (JSONObject)tagx;
                Object key = tag.get("key");
                if ("diff".equals(key)) {
                    html.append("<span class=\"tag tag-diff\" style=\"background-color: ")
                            .append(converters.convertDiffColor(tag.getString("content")))
                            .append(";\">")
                            .append(converters.convertDiff(tag.getString("content")))
                            .append("</span>");
                }
                else if("custom".equals(key)) {
                    html.append("<span class=\"tag tag-custom\" style=\"background-color: #")
                            .append(tag.get("back"))
                            .append("; color: #")
                            .append(tag.get("color"))
                            .append(";\">")
                            .append(tag.get("content"))
                            .append("</span>");
                }
            });
            if(!workingProblem.getBoolean("active")) {
                html.append("<span class=\"tag tag-cannot-solve\">제출 불가</span>");
            }
            if(finalStarList.contains(Integer.toString(workingProblem.getInt("code")))) {
                html.append("<span class=\"tag tag-star\">⭐</span>");
            }
            workingProblem.put("tags", html.toString());
            processedResult.put(workingProblem);
        });
        return processedResult;
    }
}
