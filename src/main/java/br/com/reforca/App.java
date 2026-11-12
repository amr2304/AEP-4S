package br.com.reforca;

public class App {
    public static void main(String[] args) {
        try (var c = Database.open()) {
            System.out.println("Conexão MySQL ativa.");
        }
        catch (Exception e) {
            System.err.println("Falha ao conectar ao MySQL. Execute database/schema.sql e confira MYSQL_URL, MYSQL_USER e MYSQL_PASSWORD.");
            System.err.println(e.getMessage());
            return;
        }
        UsuarioDAO u = new UsuarioDAO();
        DisciplinaDAO d = new DisciplinaDAO();
        PlanoDAO p = new PlanoDAO();
        SessaoDAO s = new SessaoDAO();
        new TerminalView(new UsuarioService(u), new DisciplinaService(d), new PlanoService(p), new SessaoService(s, u, p)).iniciar();
    }

}
