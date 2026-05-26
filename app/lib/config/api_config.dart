class ApiConfig {
  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://13.209.33.104:8080/api/app',
  );

  static String get apiRootUrl {
    if (baseUrl.endsWith('/app')) {
      return baseUrl.substring(0, baseUrl.length - 4);
    }
    return baseUrl;
  }
}
