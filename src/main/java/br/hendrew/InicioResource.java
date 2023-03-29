package br.hendrew;

import br.hendrew.api.rest.bindings.ProjectListType;
import br.hendrew.api.rest.bindings.SiteListType;
import br.hendrew.api.rest.bindings.TableauCredentialsType;
import br.hendrew.api.rest.bindings.WorkbookListType;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/login")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class InicioResource {

    @Inject
    IndicadoresService indicadoresService;


    @POST
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/logar")
    public Response login(Usuario user) throws Exception {
        try {
            indicadoresService.iniciar();
            return Response.ok(indicadoresService.logar()).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }


    @GET
    @PermitAll
    @Path("/buscar-projetos")
    public ProjectListType pegarProjetos() throws Exception {
        indicadoresService.iniciar();
        return indicadoresService.pegarProjetos();
    }

    @GET
    @PermitAll
    @Path("/buscar-sites")
    public SiteListType pegarSites() throws Exception {
        indicadoresService.iniciar();
        return indicadoresService.pegarSites();
    }


    @GET
    @PermitAll
    @Path("/buscar-work-book")
    public WorkbookListType pegarWorkbookListType() throws Exception {
        indicadoresService.iniciar();
        return indicadoresService.pegarWorkbookListType();
    }
}