var EmbedRoot = React.createClass({
        handlePlotClick: function (aesState) {
            var iFrameHeight = getParameterByName('frameHeight') ? getParameterByName('frameHeight') : 1000;
            var formHeight = getParameterByName('formHeight') ? getParameterByName('formHeight') : document.getElementById('xvsy-form').offsetHeight;
            var formWidth = getParameterByName('formWidth') ? getParameterByName('formWidth') : document.getElementById('plot-result').offsetWidth;
            this.setState({plotInFlight: true});
            $.ajax('/api/v1/plot', {
                type: "GET",
                data: {"spec": JSON.stringify(aesState),
                    "height": iFrameHeight - formHeight,
                    "width": formWidth
                },
                contentType: 'application/json; charset=utf-8',
                dataType: "html",
                success: function(data, status, req) {
                    this.setState({svg: data, plotInFlight: false});
                }.bind(this),
                error: function(xhr, status, err) {
                    console.error('api plot', status, err.toString());
                    this.setState({plotInFlight: false});
                }.bind(this)
            });
        },
        handlePlotSpec : function (spec) {
            console.log('in handleplotspec');
            this.setState({dataset: spec.dataset});
        },
        getInitialState: function () {
            return {
                svg: null,
                table: [],
                dataset: null,
                plotInFlight: true
            }
        },
        componentDidMount: function () {
        },
        render: function () {
            console.log('rendering root');
            return (
                <div className="row">
                    <div className="col-md-12">
                        <PlotResult data={this.state.svg}/>
                    </div>

                    <hr />
                    {this.state.plotInFlight ? (
                        <div className="row">
                            <div className="col-md-12">
                                <LoadingSpinner />
                            </div>
                        </div>) : null}
                    <div className="row" style={{visibility: (this.state.plotInFlight ? "hidden" : "")}} >
                        <div className="col-md-12">
                                <EmbedForm handlePlotClick={this.handlePlotClick}
                                           handlePlotSpec={this.handlePlotSpec} />
                        </div>
                    </div>
                </div>
            );
        }}
);

var LinearGeomRadios = React.createClass({
    render: function () {
        return (
            <div>
                <AesPresetRadio value="histogram" thumbnail="/geom_histogram.png" />
                <AesPresetRadio value="stacked-bar" thumbnail="/geom_bar.png" />
                <AesPresetRadio value="dodged-bar" thumbnail="/geom_bar.png" />
                <AesPresetRadio value="bin2d" thumbnail="/heatmap.png" />
                <AesPresetRadio value="point" thumbnail="/heatmap.png" />
            </div>
        );
    }
});

var VerticalAesInput = React.createClass({
    mixins: [AesInputMixin, StatOptsMixin],
    render: function () {
        var aes = this.props.aes;
        return (
            <div>
                <div className="aes-input form-horizontal">
                    <div className="form-group">
                        <label className="col-sm-3 control-label" htmlFor={this.colId(aes)}>{aes}</label>
                        <div className="col-sm-9">
                            <ColumnSelector id={this.colId(aes)} cols={this.props.schema.col}
                                            handleAesInput={this.handleAesInput} value={this.props.current ? this.props.current.col.name : ""}/>
                        </div>
                    </div>
                    <div className="form-group">
                        <label className="col-sm-3 control-label" htmlFor={this.statId(aes)}>stat</label>
                        <div className="col-sm-9">
                            {this.props.schema.stat ?
                                <StatSelector id={this.statId(this.props.current.stat)} schema={this.props.schema.stat}
                                              handleAesInput={this.handleAesInput} stat={this.props.current.stat} />
                                : null}
                        </div>
                    </div>
                    <div>
                        {this.props.schema.stat ?
                            this.getOpts()
                            : null}
                    </div>
                </div>
            </div>
        );
    }
});

var GeomDropdown = React.createClass({
    propTypes: {
        geom: React.PropTypes.string.isRequired,
        schema: React.PropTypes.array.isRequired,
        handleInput: React.PropTypes.func.isRequired
    },
    genOnGeomClick: function(geom) {
        return function () {
            var updater = {geom: {}};
            updater.geom['$set'] = geom;
            this.props.handleInput(updater);
        }.bind(this);
    },
    render: function () {
        return (
            <div className="dropdown">
                <button className="btn btn-default dropdown-toggle" type="button" id="geomDropDownMenu"
                        data-toggle="dropdown" aria-expanded="true">
                    {this.props.geom}
                    <span className="caret"></span>
                </button>
                <ul className="dropdown-menu" role="menu" aria-labelledby="geomDropDownMenu">
                    {this.props.schema.map(function (geom) {
                        return (
                            <li key={geom} role="presentation">
                                <a onClick={this.genOnGeomClick(geom)} role="menuitem" tabIndex="-1" href="#">{geom}</a>
                            </li>
                        );
                    }.bind(this))}
                </ul>
            </div>
        );
    }
});

// TODO: prettify optional components.
// these boxes are too coupled to separate into clean geom / whatever layouts.
// all layout should occur in this class: no intermediate layout components!
var EmbedForm = React.createClass({
    mixins: [FormMixin, UrlInitState],

    renderWhereSelectors: function () {
        console.log('rendering where selectors')
        return whereKeys.map(function (whereKey) {
            if (this.state.current.where && this.state.current.where[whereKey]) {
                console.log('in renderwhere for wherekey=' +whereKey);
                var inner = (<WhereSelector schema={this.state.schema.where[whereKey]}
                                            where={this.state.current.where[whereKey]}
                                            whereKey={whereKey}
                                            handleInput={this.handleInput} />);
                var remove = {where: {}};
                remove.where[whereKey] = {$set: null};
                return (
                    <RemovableWrapper key={whereKey} innerComponent={inner} remove={remove}
                                      updater={this.handleInput} />
                );
            }
            return null;
        }.bind(this));
    },
    renderRequiredAesthetics: function () {
        return AestheticKeys.map(function (aes) {
            if (this.state.schema.aesthetics[aes] && !this.state.schema.aesthetics[aes].optional) {
                return (
                    <VerticalAesInput key={aes} aes={aes} current={this.state.current.aesthetics[aes]}
                                      schema={this.state.schema.aesthetics[aes]}
                                      handleInput={this.handleInput} />
                );
            } else {
                return null;
            }
        }.bind(this));
    },
    renderOptionalAesthetics: function () {
        return AestheticKeys.map(function (aes) {
            if (this.state.current.aesthetics[aes] && this.state.schema.aesthetics[aes] && this.state.schema.aesthetics[aes].optional) {
                var inner = <VerticalAesInput aes={aes} current={this.state.current.aesthetics[aes]}
                                              schema={this.state.schema.aesthetics[aes]}
                                              handleInput={this.handleInput} />;
                var remove = {aesthetics: {}};
                remove.aesthetics[aes] = {$set: null};
                return (
                    <RemovableWrapper key={aes} innerComponent={inner} remove={remove}
                                      updater={this.handleInput}/>
                );
            } else {
                return null;
            }
        }.bind(this));
    },
    renderControlBar: function () {
        var btn_padding = {padding: "5px"};

        return (
            <div className="row" id="control-bar">
                <div className="col-md-8 vertical-center">
                    <div style={btn_padding}>
                        <GeomDropdown handleInput={this.handleInput}
                                      geom={this.state.current.geom}
                                      schema={this.state.schema.geom}></GeomDropdown>
                    </div>
                    <div id="aesthetic-adder" style={btn_padding}>
                        <AestheticAdder aesthetics={this.state.current.aesthetics}
                                        schema={this.state.schema.aesthetics}
                                        handleInput={this.handleInput}
                                        dataset={this.state.current.dataset}></AestheticAdder>
                    </div>

                    <div style={btn_padding}>
                        <WhereAdder where={this.state.current.where}
                                    schema={this.state.schema.where} handleInput={this.handleInput} />
                    </div>
                </div>
                <div className="col-md-2 col-md-offset-2 vertical-center">
                    <FetchPlot submit={this.state.current.submit} handlePlotClick={this.handlePlotClick} />
                </div>
            </div>);
    },

    render: function () {
        var whereSelectors = this.renderWhereSelectors();
        var requiredAesthetics = this.renderRequiredAesthetics();
        var optionalAesthetics = this.renderOptionalAesthetics();

        var aestheticWheres = requiredAesthetics.concat(optionalAesthetics, whereSelectors).filter(function (x){
            return (x !== null);
        });
        return (
            <div id="xvsy-form">
                <div className="row">
                    <div className="col-md-6 col-md-offset-3">
                        {this.renderControlBar()}
                    </div>
                </div>

                <div className="row">
                    {aestheticWheres.length < 4 ? (<div className="col-md-1"></div>) : null}
                    {aestheticWheres.map(function (component) {
                        if (component === null) {
                            return null;
                        }
                        return (<div key={component.key} className="col-md-3 ">{component}</div>);
                    })}
                </div>

                <hr />

            </div>
        );
    }
});
React.render(<EmbedRoot />, document.getElementById('content'));
