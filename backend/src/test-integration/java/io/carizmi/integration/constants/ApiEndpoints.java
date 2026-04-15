package io.carizmi.integration.constants;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ApiEndpoints {

    private ApiEndpoints() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static final class Members {
        public static final String ADD = "/members/add";
        public static final String UPDATE = "/members/update";
        public static final String SEARCH = "/members/search";
        public static final String LOOKUP = "/members/lookup";

        public static String get(long memberId) {
            return "/members/get/%d".formatted(memberId);
        }

        public static String delete(long memberId) {
            return "/members/delete/%d".formatted(memberId);
        }

        public static String summary(long memberId) {
            return "/members/%d/summary".formatted(memberId);
        }

        private Members() {
        }
    }

    public static final class Payments {
        public static final String ADD = "/payments/add";
        public static final String UPDATE = "/payments/update";
        public static final String SEARCH = "/payments/search";
        public static final String LATEST = "/payments/latest";

        public static String get(long paymentId) {
            return "/payments/get/%d".formatted(paymentId);
        }

        public static String delete(long paymentId) {
            return "/payments/delete/%d".formatted(paymentId);
        }

        private Payments() {
        }
    }

    public static final class Expenses {
        public static final String ADD = "/expenses/add";
        public static final String UPDATE = "/expenses/update";
        public static final String SEARCH = "/expenses/search";

        public static String get(long expenseId) {
            return "/expenses/get/%d".formatted(expenseId);
        }

        public static String delete(long expenseId) {
            return "/expenses/delete/%d".formatted(expenseId);
        }

        private Expenses() {
        }
    }

    public static final class Dashboard {
        public static final String METRICS = "/dashboard/metrics";

        private Dashboard() {
        }
    }

    public static final class Settings {
        public static final String ADD = "/system-settings/add";
        public static final String UPDATE = "/system-settings/update";
        public static final String SEARCH = "/system-settings/search";

        public static String get(long settingId) {
            return "/system-settings/get/%d".formatted(settingId);
        }

        public static String getByKey(String key) {
            return "/system-settings/by-key/%s".formatted(encode(key));
        }

        public static String delete(long settingId) {
            return "/system-settings/delete/%d".formatted(settingId);
        }

        private Settings() {
        }
    }

    public static final class Reference {
        public static final String SEARCH = "/reference/search";

        public static String getByName(String referenceName) {
            return "/reference/list/%s".formatted(encode(referenceName));
        }

        private Reference() {
        }
    }

    public static final class Checklist {
        public static final String QUARTERLY_FEE = "/checklist/quarterly-fee";

        public static String quarterlyFee(int year) {
            return "/checklist/quarterly-fee?year=%d".formatted(year);
        }

        private Checklist() {
        }
    }

    public static final class Auth {
        public static final String LOGIN = "/auth/login";
        public static final String REGISTER = "/auth/register";
        public static final String REFRESH = "/auth/refresh";
        public static final String LOGOUT = "/auth/logout";
        public static final String PROFILE = "/auth/profile";
        public static final String PASSWORD_UPDATE = "/auth/password-update";

        private Auth() {
        }
    }

    public static final class Users {
        public static final String LIST = "/users";

        public static String updateRole(long userId) {
            return "/users/%d/role".formatted(userId);
        }

        public static String updateStatus(long userId) {
            return "/users/%d/status".formatted(userId);
        }

        private Users() {
        }
    }
}