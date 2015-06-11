var XvsyRoot = React.createClass({
        handlePlotClick: function (aesState) {
            $.ajax('/api/v1/plot', {
                type: "GET",
                data: {"spec": JSON.stringify(aesState),
                    "height": document.getElementById('plot-result').clientHeight,
                    "width": document.getElementById('plot-result').clientWidth,
                    "inline": true
                },
                contentType: 'application/json; charset=utf-8',
                dataType: "html",
                success: function(data, status, req) {
                    this.setState({svg: data});
                }.bind(this),
                error: function(xhr, status, err) {
                    console.error('api plot', status, err.toString());
                }.bind(this)
            });
        },
        handleSpecUpdate : function (spec) {
            this.setState({dataset: spec.dataset});
        },
        getInitialState: function () {
            return {
                svg: null,
                table: [],
                dataset: null
            }
        },
        componentDidMount: function () {
        },
        render: function () {
            return (
                <div className="row">
                    <div className="col-md-3">
                        <SidePanelForm handlePlotClick={this.handlePlotClick} handlePlotSpec={this.handleSpecUpdate} />
                    </div>
                    <div className="col-md-9">
                        <div className="row">
                            <div className="col-md-12">
                                <PlotResult data={this.state.svg}/>
                            </div>
                        </div>
                        <div className="row">
                            <div className="col-md-12">
                                {this.state.dataset ? <DatasetSummary dataset={this.state.dataset}/> : null }
                            </div>
                        </div>
                    </div>
                </div>);
        }}
);

var SidePanelForm = React.createClass({
    mixins: [FormMixin, UrlInitState],
    render: function () {
        var dataset = this.state.schema.dataset ?
            <DatasetSelector handleInput={this.handleInput} value={this.state.current.dataset}
                             vals={this.state.schema.dataset}/> : null;
        var whereSelectors = (<WhereSelectors schema={this.state.schema.where ? this.state.schema.where : null}
                                              where={this.state.current.where ? this.state.current.where : null}
                                              handleInput={this.handleInput} />);
        return (
            <div id="xvsy-form">
                <div className="form-group">
                    <label className="h4" htmlFor="dataset">Dataset</label>
                    <div id="dataset">
                        {dataset}
                    </div>
                </div>

                <hr />

                <div className="form-group">
                    <label className="h4" htmlFor="geom-form">Geom</label>
                    <GeomSelector id="geom-form" handleInput={this.handleInput} geom={this.state.current.geom}
                        children={<GeomArray />} />
                </div>

                <hr />

                <div className="form-group">
                    <label className="h4" htmlFor="aes-form">Aesthetics</label>
                    {this.state.schema.aesthetics ?
                        <Aesthetics id="aes-form" handleInput={this.handleInput} aesthetics={this.state.current.aesthetics}
                                    schema={this.state.schema.aesthetics} /> : null}
                </div>

                <hr />

                <div className="form-group">
                    <label className="h4" htmlFor="where-form">Filters</label>
                    {this.state.schema.where ?
                        <OptionalWrapper innerComponent={whereSelectors} remove={{where: {$set: null}}}
                                         add={{where: {$set: {}}}} name="Filters" display={this.state.current.where ? true : false}
                                         updater={this.handleInput} /> : null}
                </div>
                <FetchPlot submit={this.state.current.submit} handlePlotClick={this.handlePlotClick} />
            </div>
        );
    }
});

var GeomArray = React.createClass({
    render: function() {
        return (
        <div>
            <div className="row">
                <div className="col-md-4">
                    <AesPresetRadio value="histogram" thumbnail="/geom_histogram.png" />
                </div>
                <div className="col-md-4">
                    <AesPresetRadio value="stacked-bar" thumbnail="/geom_bar.png" />
                </div>
                <div className="col-md-4">
                    <AesPresetRadio value="dodged-bar" thumbnail="/geom_bar.png" />
                </div>
            </div>
            <div className="row">
                <div className="col-md-4">
                    <AesPresetRadio value="bin2d" thumbnail="/heatmap.png" />
                </div>
                <div className="col-md-4">
                    <AesPresetRadio value="point" thumbnail="/heatmap.png" />
                </div>
                <div className="col-md-4"></div>
            </div>
        </div>
        );
    }
});
var root = React.createElement(XvsyRoot);
React.render(root, document.getElementById('content'));
