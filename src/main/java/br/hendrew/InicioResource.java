package br.hendrew;

import br.hendrew.api.rest.bindings.TableauCredentialsType;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/api/login")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class InicioResource {

    @Inject
    IndicadoresService indicadoresService;


    @POST
    @PermitAll
    @Path("/logar")
    public TableauCredentialsType login() throws Exception {
        indicadoresService.iniciar();
        return indicadoresService.logar();
    }
}