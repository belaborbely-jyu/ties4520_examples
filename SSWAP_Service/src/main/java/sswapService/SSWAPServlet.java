package sswapService;

import info.sswap.api.servlet.SimpleSSWAPServlet;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

@WebServlet(
        name = "SSWAPService",
        description = "SSWAP Service",
        urlPatterns = {"/getService"},
        initParams = {
                @WebInitParam(name = "RDGPath", value = "res/mySSWAPServiceRDG")
        }
)
public class SSWAPServlet extends SimpleSSWAPServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public <T> Class<T> getServiceClass() {

        return (Class<T>) SSWAPServ.class;
    }

}
