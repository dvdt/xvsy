var EmbedRoot = React.createClass({displayName: "EmbedRoot",
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
                React.createElement("div", {className: "row"}, 
                    React.createElement("div", {className: "col-md-12"}, 
                        React.createElement(PlotResult, {data: this.state.svg})
                    ), 

                    React.createElement("hr", null), 
                    this.state.plotInFlight ? (
                        React.createElement("div", {className: "row"}, 
                            React.createElement("div", {className: "col-md-12"}, 
                                React.createElement(LoadingSpinner, null)
                            )
                        )) : null, 
                    React.createElement("div", {className: "row", style: {visibility: (this.state.plotInFlight ? "hidden" : "")}}, 
                        React.createElement("div", {className: "col-md-12"}, 
                                React.createElement(EmbedForm, {handlePlotClick: this.handlePlotClick, 
                                           handlePlotSpec: this.handlePlotSpec})
                        )
                    )
                )
            );
        }}
);

var LinearGeomRadios = React.createClass({displayName: "LinearGeomRadios",
    render: function () {
        return (
            React.createElement("div", null, 
                React.createElement(AesPresetRadio, {value: "histogram", thumbnail: "/geom_histogram.png"}), 
                React.createElement(AesPresetRadio, {value: "stacked-bar", thumbnail: "/geom_bar.png"}), 
                React.createElement(AesPresetRadio, {value: "dodged-bar", thumbnail: "/geom_bar.png"}), 
                React.createElement(AesPresetRadio, {value: "bin2d", thumbnail: "/heatmap.png"}), 
                React.createElement(AesPresetRadio, {value: "point", thumbnail: "/heatmap.png"})
            )
        );
    }
});

var VerticalAesInput = React.createClass({displayName: "VerticalAesInput",
    mixins: [AesInputMixin, StatOptsMixin],
    render: function () {
        var aes = this.props.aes;
        return (
            React.createElement("div", null, 
                React.createElement("div", {className: "aes-input form-horizontal"}, 
                    React.createElement("div", {className: "form-group"}, 
                        React.createElement("label", {className: "col-sm-3 control-label", htmlFor: this.colId(aes)}, aes), 
                        React.createElement("div", {className: "col-sm-9"}, 
                            React.createElement(ColumnSelector, {id: this.colId(aes), cols: this.props.schema.col, 
                                            handleAesInput: this.handleAesInput, value: this.props.current ? this.props.current.col.name : ""})
                        )
                    ), 
                    React.createElement("div", {className: "form-group"}, 
                        React.createElement("label", {className: "col-sm-3 control-label", htmlFor: this.statId(aes)}, "stat"), 
                        React.createElement("div", {className: "col-sm-9"}, 
                            this.props.schema.stat ?
                                React.createElement(StatSelector, {id: this.statId(this.props.current.stat), schema: this.props.schema.stat, 
                                              handleAesInput: this.handleAesInput, stat: this.props.current.stat})
                                : null
                        )
                    ), 
                    React.createElement("div", null, 
                        this.props.schema.stat ?
                            this.getOpts()
                            : null
                    )
                )
            )
        );
    }
});

var GeomDropdown = React.createClass({displayName: "GeomDropdown",
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
            React.createElement("div", {className: "dropdown"}, 
                React.createElement("button", {className: "btn btn-default dropdown-toggle", type: "button", id: "geomDropDownMenu", 
                        "data-toggle": "dropdown", "aria-expanded": "true"}, 
                    this.props.geom, 
                    React.createElement("span", {className: "caret"})
                ), 
                React.createElement("ul", {className: "dropdown-menu", role: "menu", "aria-labelledby": "geomDropDownMenu"}, 
                    this.props.schema.map(function (geom) {
                        return (
                            React.createElement("li", {key: geom, role: "presentation"}, 
                                React.createElement("a", {onClick: this.genOnGeomClick(geom), role: "menuitem", tabIndex: "-1", href: "#"}, geom)
                            )
                        );
                    }.bind(this))
                )
            )
        );
    }
});

// TODO: prettify optional components.
// these boxes are too coupled to separate into clean geom / whatever layouts.
// all layout should occur in this class: no intermediate layout components!
var EmbedForm = React.createClass({displayName: "EmbedForm",
    mixins: [FormMixin, UrlInitState],

    renderWhereSelectors: function () {
        console.log('rendering where selectors')
        return whereKeys.map(function (whereKey) {
            if (this.state.current.where && this.state.current.where[whereKey]) {
                console.log('in renderwhere for wherekey=' +whereKey);
                var inner = (React.createElement(WhereSelector, {schema: this.state.schema.where[whereKey], 
                                            where: this.state.current.where[whereKey], 
                                            whereKey: whereKey, 
                                            handleInput: this.handleInput}));
                var remove = {where: {}};
                remove.where[whereKey] = {$set: null};
                return (
                    React.createElement(RemovableWrapper, {key: whereKey, innerComponent: inner, remove: remove, 
                                      updater: this.handleInput})
                );
            }
            return null;
        }.bind(this));
    },
    renderRequiredAesthetics: function () {
        return AestheticKeys.map(function (aes) {
            if (this.state.schema.aesthetics[aes] && !this.state.schema.aesthetics[aes].optional) {
                return (
                    React.createElement(VerticalAesInput, {key: aes, aes: aes, current: this.state.current.aesthetics[aes], 
                                      schema: this.state.schema.aesthetics[aes], 
                                      handleInput: this.handleInput})
                );
            } else {
                return null;
            }
        }.bind(this));
    },
    renderOptionalAesthetics: function () {
        return AestheticKeys.map(function (aes) {
            if (this.state.current.aesthetics[aes] && this.state.schema.aesthetics[aes] && this.state.schema.aesthetics[aes].optional) {
                var inner = React.createElement(VerticalAesInput, {aes: aes, current: this.state.current.aesthetics[aes], 
                                              schema: this.state.schema.aesthetics[aes], 
                                              handleInput: this.handleInput});
                var remove = {aesthetics: {}};
                remove.aesthetics[aes] = {$set: null};
                return (
                    React.createElement(RemovableWrapper, {key: aes, innerComponent: inner, remove: remove, 
                                      updater: this.handleInput})
                );
            } else {
                return null;
            }
        }.bind(this));
    },
    renderControlBar: function () {
        var btn_padding = {padding: "5px"};

        return (
            React.createElement("div", {className: "row", id: "control-bar"}, 
                React.createElement("div", {className: "col-md-8 vertical-center"}, 
                    React.createElement("div", {style: btn_padding}, 
                        React.createElement(GeomDropdown, {handleInput: this.handleInput, 
                                      geom: this.state.current.geom, 
                                      schema: this.state.schema.geom})
                    ), 
                    React.createElement("div", {id: "aesthetic-adder", style: btn_padding}, 
                        React.createElement(AestheticAdder, {aesthetics: this.state.current.aesthetics, 
                                        schema: this.state.schema.aesthetics, 
                                        handleInput: this.handleInput, 
                                        dataset: this.state.current.dataset})
                    ), 

                    React.createElement("div", {style: btn_padding}, 
                        React.createElement(WhereAdder, {where: this.state.current.where, 
                                    schema: this.state.schema.where, handleInput: this.handleInput})
                    )
                ), 
                React.createElement("div", {className: "col-md-2 col-md-offset-2 vertical-center"}, 
                    React.createElement(FetchPlot, {submit: this.state.current.submit, handlePlotClick: this.handlePlotClick})
                )
            ));
    },

    render: function () {
        var whereSelectors = this.renderWhereSelectors();
        var requiredAesthetics = this.renderRequiredAesthetics();
        var optionalAesthetics = this.renderOptionalAesthetics();

        var aestheticWheres = requiredAesthetics.concat(optionalAesthetics, whereSelectors).filter(function (x){
            return (x !== null);
        });
        return (
            React.createElement("div", {id: "xvsy-form"}, 
                React.createElement("div", {className: "row"}, 
                    React.createElement("div", {className: "col-md-6 col-md-offset-3"}, 
                        this.renderControlBar()
                    )
                ), 

                React.createElement("div", {className: "row"}, 
                    aestheticWheres.length < 4 ? (React.createElement("div", {className: "col-md-1"})) : null, 
                    aestheticWheres.map(function (component) {
                        if (component === null) {
                            return null;
                        }
                        return (React.createElement("div", {key: component.key, className: "col-md-3 "}, component));
                    })
                ), 

                React.createElement("hr", null)

            )
        );
    }
});
React.render(React.createElement(EmbedRoot, null), document.getElementById('content'));
