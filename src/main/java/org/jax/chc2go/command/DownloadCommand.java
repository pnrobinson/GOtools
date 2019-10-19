package org.jax.chc2go.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jax.chc2go.io.Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Download a number of files needed for the analysis. We download by default to a subdirectory called
 * {@code data}, which is created if necessary. We download the files {@code hp.obo}, {@code phenotype.hpoa},
 * {@code Homo_sapiencs_gene_info.gz}, and {@code mim2gene_medgen}.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Download files for LIRICAL")
public class DownloadCommand extends Chc2GoCommand {
    private static final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);

    @Parameter(names={"-d","--data"}, description ="directory to download data (default: data)" )
    private String datadir="data";
    @Parameter(names={"-w","--overwrite"}, description = "overwrite prevously downloaded files, if any")
    private boolean overwrite;

    public DownloadCommand() {
    }


    @Override
    public void run() {
        logger.info(String.format("Download analysis to %s", datadir));
        Downloader downloader = new Downloader(datadir, overwrite);
        downloader.download();
    }
}

