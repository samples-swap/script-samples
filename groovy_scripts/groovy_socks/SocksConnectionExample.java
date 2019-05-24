class SocksConnectionExample { 

  public static void main(String[] args) throws Exception {
    Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory> create()
            .register("http", new MyConnectionSocketFactory())
            .register("https", new MySSLConnectionSocketFactory(SSLContexts.createSystemDefault())).build();
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg, new FakeDnsResolver());
    CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(cm).build();
    try {
        InetSocketAddress socksaddr = new InetSocketAddress("mysockshost", 1234);
        HttpClientContext context = HttpClientContext.create();
        context.setAttribute("socks.address", socksaddr);

        HttpGet request = new HttpGet("https://www.funnyordie.com");

        System.out.println("Executing request " + request + " via SOCKS proxy " + socksaddr);
        CloseableHttpResponse response = httpclient.execute(request, context);
        try {
            System.out.println("----------------------------------------");
            System.out.println(response.getStatusLine());
            int i = -1;
            InputStream stream = response.getEntity().getContent();
            while ((i = stream.read()) != -1) {
                System.out.print((char) i);
            }
            EntityUtils.consume(response.getEntity());
        } finally {
            response.close();
        }
    } finally {
        httpclient.close();
    }
}