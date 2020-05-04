package com.ggstudios.tools.datafixer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChampionInfoFixer {

    private static final Pattern SPECIAL_VARIABLE_PATTERN = Pattern.compile("\\{\\{ ([a-z0-9.*]+) \\}\\}");

	public static JSONObject loadJsonObj(File file) throws JSONException, IOException {
		// Load the JSON object containing champ data first...
		InputStream is = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		StringBuilder builder = new StringBuilder();
		String readLine = null;

		// While the BufferedReader readLine is not null
		while ((readLine = br.readLine()) != null) {
			builder.append(readLine);
		}

		// Close the InputStream and BufferedReader
		is.close();
		br.close();

		return new JSONObject(builder.toString());
	}

	private static final String STAT_TOTAL_AP = "spelldamage";
	private static final String STAT_GENERIC_VALUE = "STAT_GENERIC_VALUE";
	private static final String STAT_TOTAL_AD = "attackdamage";
	private static final String STAT_BONUS_AD = "bonusattackdamage";
	private static final String STAT_KLED_CHARGE_SCALING = "STAT_KLED_CHARGE_SCALING";

	public static JSONObject fix(JSONObject champData) {
        int champId = champData.getInt("id");
		JSONArray skillsJson = champData.getJSONArray("spells");

		final int len = skillsJson.length();
		for (int i = 0; i < len; i++) {
            fixSkill(champId, skillsJson.getJSONObject(i));
        }

		return champData;
	}

	private static boolean isPositiveInt(String possibleInt) {
        for (int i = 0; i < possibleInt.length(); i++) {
            if (!Character.isDigit(possibleInt.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static void addVar(JSONObject skillObj, String varName, String link, double... coeff) {
        JSONArray varsArr = skillObj.optJSONArray("vars");
        if (varsArr == null) {
            varsArr = new JSONArray();
            skillObj.put("vars", varsArr);
        }

        JSONObject o = new JSONObject();
        o.put("key", varName);
        o.put("link", link);

        JSONArray coeffArr = new JSONArray();
        for (double d : coeff) {
            coeffArr.put(d);
        }
        o.put("coeff", coeffArr);

        varsArr.put(o);
    }

    private static void fixSkill(int champId, JSONObject skillObj) {
        Set<String> varNames = new HashSet<>();

        JSONArray vars = skillObj.optJSONArray("vars");

        if (vars != null) {
            for (int j = 0; j < vars.length(); j++) {
                JSONObject var = vars.getJSONObject(j);
                varNames.add(var.getString("key"));
            }
        }

        Matcher matcher = SPECIAL_VARIABLE_PATTERN.matcher(skillObj.getString("tooltip"));

        while (matcher.find()) {
            String varName = matcher.group(1);

            if (varNames.contains(varName)) {
                continue;
            }
            if (varName.startsWith("e")) {
                String possibleInt = varName.substring(1);

                if (isPositiveInt(possibleInt)) {
                    continue;
                }
            }

            //Log.d(TAG, "VarName: " + varName);


            switch (champId) {
                case 3: // galio
                    switch (varName) {
                        case "charabilitypower2*3":
                            addVar(skillObj, varName, STAT_TOTAL_AP, .60);
                            break;
                    }
                    break;
                case 27: // singed
                    switch (varName) {
                        case "charabilitypower*4":
                            addVar(skillObj, varName, STAT_TOTAL_AP, .40);
                            break;
                        case "charbonusphysical*2":
                            addVar(skillObj, varName, STAT_TOTAL_AP, .40);
                            break;
                    }
                    break;
//                case 30: // Karthus
//                    switch (varName) {
//                        case "cost":                return skillObj.getString("costBurn"); break;
//                    }
//                    break;
                case 36: // DrMundo
                    switch (varName) {
                        case "f3*100":
                            addVar(skillObj, varName, STAT_GENERIC_VALUE, 6, 12, 18, 24, 30);
                            break;
                    }
                    break;
                case 37: // Sona
                    switch (varName) {
                        case "f1*100":
                            addVar(skillObj, varName, STAT_GENERIC_VALUE, 0);
                            break;
                        case "f2*100":
                            addVar(skillObj, varName, STAT_GENERIC_VALUE, 6);
                            break;
                        case "f3*100":
                            addVar(skillObj, varName, STAT_GENERIC_VALUE, 0);
                            break;
                    }
                    break;
				case 50: // Swain
					break;
                case 55: // Katarina
                    switch (varName) {
                        case "f3*100":
                            addVar(skillObj, varName, STAT_GENERIC_VALUE, 78, 84, 90, 96);
                            break;
                    }
                    break;
//                case 69: // Cassiopeia
//                    switch (varName) {
//                        case "cost":                return costBurn;
//                    }
//                    break;
                case 115: // Ziggs
                    switch (varName) {
                        case "f1*100":
                            addVar(skillObj, varName, STAT_GENERIC_VALUE, 25, 27.5, 30, 32.5, 35);
                            break;
                    }
                    break;
                case 142: // zoe
                    switch (varName) {
                        case "f1*2.5":
                            addVar(skillObj, varName, STAT_TOTAL_AP, .66);
                            break;
                    }
                    break;
                case 143: // zyra
                    switch (varName) {
                        case "ammorechargetime":
                            addVar(skillObj, varName, STAT_GENERIC_VALUE, 20, 18, 16, 14, 12);
                            break;
                    }
                    break;
                case 240: // kled
                    switch (varName) {
                        case "charbonusphysical*2":
                            addVar(skillObj, varName, STAT_BONUS_AD, 1.2);
                            break;
                        case "f1*3":
                            addVar(skillObj, varName, STAT_KLED_CHARGE_SCALING, .12);
                            break;
                    }
                    break;
                case 266: // Aatrox
                    switch (varName) {
                        case "qbasedamage":
                            addVar(skillObj, varName, STAT_GENERIC_VALUE, 20, 35, 50, 65, 80);
                            break;
                        case "qtotaladratiodamage":
                            addVar(skillObj, varName, STAT_TOTAL_AD, .64, .68, .72, .76, .80);
                            break;
                    }
                    break;
                case 268: // Azir
                    switch (varName) {
                        case "maxammo":
                            addVar(skillObj, varName, STAT_GENERIC_VALUE, 2);
                            break;
                    }
                    break;
                case 420: // Illaoi
                    switch (varName) {
                        case "f1*100":
                            addVar(skillObj, varName, STAT_GENERIC_VALUE, 0);
                            break;
                        case "f2*100":
                            addVar(skillObj, varName, STAT_GENERIC_VALUE, 2);
                            break;
                    }
                    break;
            }
        }
    }
}
