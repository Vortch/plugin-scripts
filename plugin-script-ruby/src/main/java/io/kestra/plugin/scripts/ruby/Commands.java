package io.kestra.plugin.scripts.ruby;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.runners.ScriptService;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.AbstractExecScript;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Execute a Ruby script from the Command Line Interface."
)
@Plugin(examples = {
    @Example(
        full = true,
        title = """
        Create a Ruby script and execute it. The easiest way to create a Ruby script is to use the embedded VS Code editor. Create a file named `main.rb` and paste the following code:
        
        ```ruby
        require 'csv'
        require 'json'

        file = File.read('data.json')
        data_hash = JSON.parse(file)

        # Extract headers
        headers = data_hash.first.keys

        # Convert hashes to arrays
        data = data_hash.map(&:values)

        # Prepend headers to data
        data.unshift(headers)

        # Create and write data to CSV file
        CSV.open('output.csv', 'wb') do |csv|
        data.each { |row| csv << row }
        end
        ```
        
        In order to read that script from the [Namespace File](https://kestra.io/docs/developer-guide/namespace-files) called `main.rb`, you need to enable the `namespaceFiles` property. We include only `main.rb` as that is the only file we want from the `namespaceFiles`.
        
        Also, note how we use the `inputFiles` option to read additional files into the script's working directory. In this case, we read the `data.json` file, which contains the data that we want to convert to CSV.
        
        Finally, we use the `outputFiles` option to specify that we want to output the `output.csv` file that is generated by the script. This allows us to access the file in the UI's Output tab and download it, or pass it to other tasks.
        """,
        code = """
            id: generate_csv
            namespace: company.team
            
            tasks:
              - id: bash
                type: io.kestra.plugin.scripts.ruby.Commands
                namespaceFiles:
                  enabled: true
                  include:
                    - main.rb
                inputFiles:
                  data.json: |
                    [
                        {"Name": "Alice", "Age": 30, "City": "New York"},
                        {"Name": "Bob", "Age": 22, "City": "Los Angeles"},
                        {"Name": "Charlie", "Age": 35, "City": "Chicago"}
                    ]
                beforeCommands:
                  - ruby -v
                commands:
                  - ruby main.rb
                outputFiles:
                  - "*.csv"
            """
    )
})
public class Commands extends AbstractExecScript {
    private static final String DEFAULT_IMAGE = "ruby";

    @Builder.Default
    protected String containerImage = DEFAULT_IMAGE;

    @Schema(
        title = "The commands to run"
    )
    @PluginProperty(dynamic = true)
    @NotEmpty
    protected List<String> commands;

    @Override
    protected DockerOptions injectDefaults(DockerOptions original) {
        var builder = original.toBuilder();
        if (original.getImage() == null) {
            builder.image(this.getContainerImage());
        }

        return builder.build();
    }

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        List<String> commandsArgs = ScriptService.scriptCommands(
            this.interpreter,
            getBeforeCommandsWithOptions(),
            this.commands,
            this.targetOS
        );

        return this.commands(runContext)
            .withCommands(commandsArgs)
            .run();
    }
}
