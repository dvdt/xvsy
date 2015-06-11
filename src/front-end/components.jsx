/** @jsx React.DOM */

/** Root node of this UI.
 * State:   [(column_name, type, factor?, uniq-elems, range), ...],
 *          [(aes, col, stat, pos, visible?), ...] */

var toSpecReq = function (spec) {
  return JSON.stringify(spec);
};

var AestheticKeys = ['x', 'y', 'facet_x', 'facet_y', 'fill'];

// from http://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
var getParameterByName = function (name) {
  name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
  var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
      results = regex.exec(location.search);
  return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
};

/**
 * These come from xvsy/ui
 * @type {string[]}
 */
var whereKeys = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];

var LoadingSpinner = React.createClass({
  render: function () {
    return (
        <div style={{width: "100%", height: "100%", backgroundColor: "red"}}>
          <h1 className="text-center">Loading ...</h1>
        </div>
    );
  }
});

var DatasetSummary = React.createClass({
  fetchDatasetHead: function(dataset) {
    $.ajax('/api/v1/head',
        {
          type: "GET",
          data: {"dataset": dataset},
          contentType: 'application/json; charset=utf-8',
          dataType: "html",
          success: function(data, status, req) {
            this.setState({dataset_head: data});
          }.bind(this),
          error: function(xhr, status, err) {
            console.error('dataset summary', status, err.toString());
          }.bind(this)
        });
  },
  getInitialState: function () {
    return {dataset_head: "<tr><th>Loading ...</th></tr>"};

  },
  componentDidMount: function() {
    this.fetchDatasetHead(this.props.dataset);
  },
  componentWillReceiveProps: function (nextProps) {
    if (this.props.dataset !== nextProps.dataset) {
      this.fetchDatasetHead(nextProps.dataset);
      this.setState({dataset_head: "<tr><th>Loading ...</th></tr>"});
    }
  },
  render: function () {
    return (
        <div className="row">
          <div className="col-md-12">
            <h3>Summary of {this.props.dataset}</h3>
            <table className="table table-condensed table-bordered" dangerouslySetInnerHTML={{__html: this.state.dataset_head}}>
            </table>
          </div>
        </div>
    );
  }
});

/**
 * Defines plot specifications that go into drawing / scaling plot after the data has been retrieved:
 * scales, width, height, legend labellers.
 */
var PlotOptions = React.createClass({
      render: function () {
        return (
            <div>
              <input />

            </div>);
      }
    }
);

var UrlInitState = {
  componentWillMount: function () {
    var parsedSpec = {
      dataset: "",
      geom: "",
      aesthetics: {},
      where: null
    };
    try {
      if (getParameterByName('spec') !== "") {
        parsedSpec = $.parseJSON(getParameterByName('spec'));
        this.setState({current: parsedSpec, schema: {dataset: [], geom: [], aesthetics: {}, where: {}}});
      }
    } catch(err) {
      console.log('parse spec query string error');
      console.log(err);
    }
  }};

var FormMixin = {
  propTypes: {
    handlePlotSpec: React.PropTypes.func.isRequired
  },

  getInitialState: function () {
    return {
      current: {
        dataset: "",
        geom: "",
        aesthetics: {},
        where: null
      },
      schema: {geom: [], dataset: [], aesthetics: {}, where: {}}};
  },

  fetchState: function (state, onSuccess) {
    $.ajax({
      url: '/api/v1/schema',
      type: "GET",
      data: {spec: JSON.stringify(state.current)},
      contentType: 'application/json; charset=utf-8',
      dataType: 'json',
      success: onSuccess,
      error: function(xhr, status, err) {
        console.error('api schema', status, err.toString());
      }.bind(this)
    });
  },

  componentDidMount: function () {
    this.fetchState(this.state, function(data) {
      var newCurrent = React.addons.update(this.state.current, {$merge: data.current});
      this.props.handlePlotSpec(newCurrent);
      this.setState({current: newCurrent, schema: data.schema});
      this.handlePlotClick();
    }.bind(this));
  },

  isExpr2Update: function (updater) {
    if (!updater.hasOwnProperty('where')) {
      return false;
    }

    var whereUpdater = updater.where;
    for (var i = 0; i < whereKeys.length; i++) {
      var k = whereKeys[i];
      if (whereUpdater[k] && whereUpdater[k].expr2)
        return true;
    }

    return false;
  },
  handleInput: function (updater) {
    var newState = React.addons.update(this.state, {current: updater});
    console.log("newState: ");
    console.log(newState);

    if (!this.isExpr2Update(updater)) {
      console.log('fetching state');
      this.fetchState(newState, function(data) {
        var newCurrent = React.addons.update(this.state.current, {$merge: data.current});
        this.props.handlePlotSpec(newCurrent);
        this.setState({current: newCurrent, schema: data.schema});
      }.bind(this));
    } else {
      this.setState(newState);
    }
  },
  handlePlotClick: function () {
    this.props.handlePlotClick(this.state.current);
  }
};

var AestheticAdder = React.createClass({
  propTypes: {
    aesthetics: React.PropTypes.object.isRequired,
    schema: React.PropTypes.object.isRequired,
    handleInput: React.PropTypes.func.isRequired,
    dataset: React.PropTypes.string.isRequired
  },
  onSelect: function (e) {
    var addedAes = e.target.value;
    if (addedAes !== "add aesthetic") {
      var updater = {aesthetics: {}};
      updater.aesthetics[addedAes] = {$set: {}};
      this.props.handleInput(updater);
    }
  },
  defaultColumns: {
    flights: {
      x: 'Month',
      fill: 'DayOfMonth',
      y: 'Month',
      facet_x: 'DayOfWeek',
      facet_y: 'UniqueCarrier'
    }
  },
  genOnClick: function (addedAes) {
    return function () {
      var column = this.defaultColumns[this.props.dataset] && this.defaultColumns[this.props.dataset][addedAes];
      var updater = {aesthetics: {}};
      var aestheticMapping = {};
      if (column) {
        aestheticMapping = {col: {}};
        aestheticMapping.col.name = column;
      }
      updater.aesthetics[addedAes] = {$set: aestheticMapping};
      this.props.handleInput(updater);
    }.bind(this);
  },
  render: function () {
    var unusedAesthetics = AestheticKeys.filter(function (aes) {
      return (this.props.schema.hasOwnProperty(aes) &&
      !this.props.aesthetics.hasOwnProperty(aes));
    }.bind(this));

    return (
        <div className="dropdown">
          <button className="btn btn-default dropdown-toggle" type="button" id="dropdownMenu1"
                  data-toggle="dropdown" aria-expanded="true">
            add aesthetic
            <span className="caret"></span>
          </button>
          <ul className="dropdown-menu" role="menu" aria-labelledby="dropdownMenu1">
            {unusedAesthetics.map(function (aes) {
              if (this.props.schema.hasOwnProperty(aes) &&
                  !this.props.aesthetics.hasOwnProperty(aes)) {
                return (
                    <li key={aes} role="presentation">
                      <a onClick={this.genOnClick(aes)} role="menuitem" tabIndex="-1" href="#">{aes}</a>
                    </li>
                );
              } else {
                return null;
              }
            }.bind(this))}
          </ul>
        </div>
    );
  }
});

var WhereAdder = React.createClass({
  propTypes: {
    where: React.PropTypes.object.isRequired,
    schema: React.PropTypes.object.isRequired,
    handleInput: React.PropTypes.func.isRequired
  },
  nextWhere: function() {
    if (!this.props.where) {
      return 'a';
    }
    var unusedWheres = whereKeys.filter(function (aes) {
      return (this.props.schema.hasOwnProperty(aes) &&
      !this.props.where.hasOwnProperty(aes));
    }.bind(this));

    return unusedWheres ? unusedWheres[0] : null;
  },
  handleAddWhereClick: function () {
    var nextWhereKey = this.nextWhere();
    var settee = {};
    settee[nextWhereKey] = {};
    if (!this.props.where) {
      this.props.handleInput({where: {$set: settee}});
    } else {
      this.props.handleInput({where: {$merge: settee}});
    }
  },
  render: function () {

    /*
    when no where clauses are enabled, this.props.where == null
     */
    if (!this.props.where || this.nextWhere()) {
      return (<button className="btn btn-default" onClick={this.handleAddWhereClick}>
        <span className="glyphicon glyphicon-plus" aria-hidden="true"></span> filter
      </button>);
    }
    return null;
  }
});

var WhereSelector = React.createClass({
  propTypes: {
    schema: React.PropTypes.object,
    where: React.PropTypes.object,
    whereKey: React.PropTypes.string,
    handleInput: React.PropTypes.func
  },

  getInitialState: function () {
    // initial expr2 gets set from query string
    var expr2 = "";
    if (this.props.where.expr2) {
      expr2 = this.props.where.expr2 instanceof Array ? this.props.where.expr2.toString() : "";
    }
    return {expr2: expr2};
  },

  componentWillReceiveProps: function(nextProps) {
    /*
    This selector needs to maintain expr2 state to display to the easier what she typed, which may differ from what
    is stored in the XvsyForm component. For example, entering "SFO, LAX" or "SFO LAX" results in the same expr2
    being stored in XvsyForm; thus, we cannot re-render expr2 in the WhereSelector based on the XvsyForm expr2 state.
    Thus, WhereSelector needs to have it's own state for user input.
     */
    var curExpr2 = JSON.stringify(this.toJsonVal(this.state.expr2));
    var newExpr2 = JSON.stringify(nextProps.where.expr2);
    console.log("cur="+curExpr2);
    console.log("new="+newExpr2);
    if (newExpr2 !== curExpr2) {
      var expr2;
      if (nextProps.where.expr2) {
        expr2 = nextProps.where.expr2 instanceof Array ? nextProps.where.expr2.toString() : nextProps.where.expr2;
      } else {
        expr2 = ""
      }
      this.setState({expr2: expr2})
    }
  },
  /**
   * input form for expr2.
   */
  expr2InputForm: function() {
    var pred = this.props.where.pred;
    var type = (pred === "in" || pred === "not-in") ? "text" : this.props.schema.expr2;
    var val = this.state.expr2;
    if (type === "boolean") {
      return (this.genSelector(val, ["True", "False"], this.handleExpr2Change));
    }
    else {
      return (<input className="form-control" type={type} value={val} onChange={this.handleExpr2Change}></input>);
    }
  },
  genSelector: function(val, options, onChange) {
    return (
        <div>
          {val ?
              <select className="form-control" value={val} onChange={onChange}>
                {options.map(function (o) {
                  return (<option value={o} key={o}>{o}</option>);
                })}
              </select> : null}
        </div>
    );
  },
  handleExpr1Change: function(e) {
    var updater = {where: {}};
    updater.where[this.props.whereKey] = {expr1: {$set: e.target.value}};
    this.props.handleInput(updater);
  },
  handlePredChange: function(e) {
    var updater = {where: {}};
    updater.where[this.props.whereKey] = {pred: {$set: e.target.value}};
    this.props.handleInput(updater);
  },

  toJsonVal: function(v) {
    var jsonVal = v;
    var pred = this.props.where.pred;
    if (pred === "in" || pred === "not-in") {
      jsonVal = v.split(/[, ]/).filter(function (s) {
        return (s !== "");
      });
    }
    return jsonVal;
  },
  handleExpr2Change: function(e) {
    console.log('handleexpr1 change');
    var changeVal = this.toJsonVal(e.target.value);
    var updater = {where: {}};
    updater.where[this.props.whereKey] = {expr2: {$set: changeVal}};

    this.state.expr2 = e.target.value;
    this.setState(this.state);
    this.props.handleInput(updater);
  },
  render: function() {
    var components = null;
    if (this.props.schema) {
      components = [
        this.genSelector(this.props.where.expr1,
            this.props.schema.expr1, this.handleExpr1Change),
        this.genSelector(this.props.where.pred,
            this.props.schema.pred, this.handlePredChange),
        this.expr2InputForm()
      ];
    }
    return (
        <div id={'where-' + this.props.whereKey} className="where-selector">
          <div  className="row form-group">
            <div className="col-md-7">
              {components ? components[0] : null}
            </div>
            <div className="col-md-5">
              {components ? components[1] : null}
            </div>
          </div>
          <div className="row">
            <div className="col-md-12 form-group">
              {components ? components[2] : null}
            </div>
          </div>

        </div>
    );
  }
});

var WhereSelectors = React.createClass({
  propTypes: {
    schema: React.PropTypes.object,
    where: React.PropTypes.object,
    handleInput: React.PropTypes.func
  },
  renderWhere: function(whereKey) {
    /* existing filters */
    var removeSpec = {where: {}};
    removeSpec.where[whereKey] = {$set: null};
    var addSpec = {where: {}};
    addSpec.where[whereKey] = {$set: {}};
    var inner = null;
    // key already exists
    if (this.props.schema[whereKey] && (this.props.where ? this.props.where[whereKey] : false)) {
      inner = (<WhereSelector schema={this.props.schema[whereKey]}
                                  where={this.props.where[whereKey]} whereKey={whereKey} handleInput={this.props.handleInput}
                                  key={whereKey} />);
      return (<OptionalWrapper innerComponent={inner} remove={removeSpec} add={addSpec} name="filter" display={true}
                               updater={this.props.handleInput} key={whereKey} />);
    }
    /* Click to add new filter */
    else if (this.props.schema[whereKey]) {
      inner = (<WhereSelector schema={this.props.schema[whereKey]}
                                  where={null} whereKey={whereKey} handleInput={this.props.handleInput}
                                  key={whereKey} />);
      return (<OptionalWrapper innerComponent={inner} remove={removeSpec} add={addSpec} name="filter" display={false}
                               updater={this.props.handleInput} key={whereKey} />);
    }
    else {return null;}
  },
  render: function() {
    return (
        <div>
          {whereKeys.map(this.renderWhere)}
        </div>);
  }
});


var DatasetSelector = React.createClass({
  handleChange: function (event) {
    var val = event.target.value;
    this.props.handleInput({"dataset": {$set: val}});
  },
  render: function () {
    return (
        <select className="form-control" onChange={this.handleChange} value={this.props.value}>
          {this.props.vals.map(function (x) {
            return (<option value={x} key={x}>{x}</option>);
          })}
        </select>
    );
  }
});

var GeomSelector = React.createClass({
  propTypes: {
    geom: React.PropTypes.string.isRequired,
    handleInput: React.PropTypes.func.isRequired,
    children: React.PropTypes.element.isRequired
  },
  handleChange: function (event) {
    var val = event.target.value;
    this.props.handleInput({"geom": {$set: val}});
  },
  render: function () {
    return (
        <RadioGroup name="aes-preset" value={this.props.geom} onChange={this.handleChange}>
          {this.props.children}
        </RadioGroup>
    );
  }
});

var Aesthetics = React.createClass({
  propTypes: {
    // schema from serve
    schema: React.PropTypes.object.isRequired,
    // aesthetics contains the current vals
    aesthetics: React.PropTypes.object.isRequired,
    // function that takes an update object, see React.addons.update
    handleInput: React.PropTypes.func.isRequired
  },
  wrapOptional: function (aes) {
    if (this.props.schema[aes] && this.props.schema[aes].optional) {
      var component = (<AesInput handleInput={this.props.handleInput}
                                aes={aes} schema={this.props.schema[aes]}
                                current={this.props.aesthetics[aes]} key={aes} />);
      var addAes= {aesthetics: {}};
      addAes.aesthetics[aes] = {$set: {}};
      var rmAes = {aesthetics: {}};
      rmAes.aesthetics[aes] = {$set: null};
      return <OptionalWrapper display={this.props.aesthetics[aes] ? true : false} updater={this.props.handleInput}
                              add={addAes} remove={rmAes} innerComponent={component} name={aes} key={aes}/>
    }
    else
      return null;
  },
  render: function () {
    var aestheticInputs = ["x", "y", "fill", "color"].map(function (aes) {
      return (this.props.schema[aes] && !this.props.schema[aes].optional ? <AesInput handleInput={this.props.handleInput}
                                                                                     aes={aes} schema={this.props.schema[aes]}
                                                                                     current={this.props.aesthetics[aes]} key={aes} /> : null)
    }.bind(this));

    var optionalAesthetics = ["x", "y", "fill", "color"].map(this.wrapOptional);

    var facet_x = (<AesInput handleInput={this.props.handleInput}
                             aes='facet_x' schema={this.props.schema['facet_x']}
                             current={this.props.aesthetics['facet_x']} key={'facet_x'} />);
    var facet_y = (<AesInput handleInput={this.props.handleInput}
                             aes='facet_y' schema={this.props.schema['facet_y']}
                             current={this.props.aesthetics['facet_y']} key={'facet_y'} />);

    return (
        <div>
          <div id="required-aesthetics">
            {aestheticInputs}
          </div>
          <div id="required-aesthetics">
            {optionalAesthetics}
          </div>
          <div id="facets">
            {this.wrapOptional('facet_x')}
            {this.wrapOptional('facet_y')}
          </div>
        </div>
    )}
});

var AesInputMixin = {
  propTypes: {
    aes: React.PropTypes.string.isRequired,
    current: React.PropTypes.object.isRequired,
    schema: React.PropTypes.object.isRequired,
    handleInput: React.PropTypes.func.isRequired
  },
  handleAesInput: function (aesInput) {
    var updater = {aesthetics: {}};
    updater.aesthetics[this.props.aes] = aesInput;
    this.props.handleInput(updater);
  },
  colId: function (aes) {return aes + "-col";},
  statId: function (aes) {return aes + "-stat";}
};

var StatOptsMixin = {
  getOpts: function() {
    var optsVal = this.props.current.stat.opts;
    var optsSchema = this.props.schema.stat.opts;
    if (optsVal && optsSchema) {
      if (optsSchema === "bin-opts") {
        return <StatOptsBin lower={optsVal.lower} upper={optsVal.upper} nbins={optsVal.nbins}
                            handleAesInput={this.handleAesInput} id={this.props.aes + "-binopts"}/>
      }
    } else if (optsVal && !optsSchema) {
      this.handleAesInput({stat: {opts: {$set: null}}});
    }

    return null;
  }
};
/**
 * AesInput displays a row of column and stat selectors. UI form for submitting data of shape
 * {aesthetics: {x: {col: {name: "col-name", factor: true},
 *  stat: {name: "stat-name", opts: {json-opts}}}}
 */
var AesInput = React.createClass({
  mixins: [AesInputMixin, StatOptsMixin],


  render: function () {
    var aes = this.props.aes;
    return (
        <div className="aes-input">
          <div className="row">
            <div className="form-group col-md-8">
              <label htmlFor={this.colId(aes)}>{aes}</label>
              <ColumnSelector id={this.colId(aes)} cols={this.props.schema.col}
                              handleAesInput={this.handleAesInput} value={this.props.current ? this.props.current.col.name : ""}/>
            </div>
            <div className="form-group col-md-4">
              <label htmlFor={this.statId(aes)}>Statistic</label>
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

        </div>);
  }
});

/**
 * Wraps child element with an "x" close button
 */
var RemovableWrapper = React.createClass({
  propTypes: {
    innerComponent: React.PropTypes.element.isRequired,
    remove: React.PropTypes.object,
    updater: React.PropTypes.func
  },
  remove: function () {
    this.props.updater(this.props.remove);
  },
  render: function () {
    var rmStyle = {
      position: 'absolute',
      top: '6px',
      left: '18px',
      color: 'red'
    };

    return (
      <div className="removable-wrapper">
        {this.props.innerComponent}
        <button type="button" className="close btn-remove" data-dismiss="modal" aria-label="Close"
                onClick={this.remove} style={rmStyle}>
          <span className="glyphicon glyphicon-remove"
                aria-hidden="true"></span>
        </button>
      </div>
    );
  }
});
/**
 * Wraps a react component that represents optional data. has two UI modes: 'open' and 'closed.' In the 'closed' state,
 * updates the responsible state to null.
 */
var OptionalWrapper = React.createClass({
  propTypes: {
    innerComponent: React.PropTypes.element.isRequired,
    remove: React.PropTypes.object,
    add: React.PropTypes.object,
    name: React.PropTypes.string,
    display: React.PropTypes.bool,
    updater: React.PropTypes.func},
  remove: function () {
    this.props.updater(this.props.remove)
  },
  add: function () {
    this.props.updater(this.props.add)
  },
  render: function() {
    return (
        <div>
          {this.props.display ?
              <button className="btn btn-danger" type="submit" onClick={this.remove}>remove {this.props.name}</button> :
              <button className="btn btn-success" type="submit" onClick={this.add}>
                add {this.props.name}</button>
          }
          {this.props.display ? this.props.innerComponent : null}
        </div>
    )
  }
});

var FetchPlot = React.createClass({
  render: function () {
    return <button className="btn-lg btn-primary" onClick={this.props.handlePlotClick}>Plot</button>;
  }
});

var ColumnOption = React.createClass({
  render: function () {
    return (<option value={this.props.opt}>{this.props.opt}</option>);
  }
});

/** Knows how to update {col: {name: "my-name", factor: false}}
 */
var ColumnSelector = React.createClass({
  handleChange: function (event) {
    var val = event.target.value;
    this.props.handleAesInput({col: {name: {$set: val}}});
  },
  render: function () {
    return (
          <select onChange={this.handleChange} value={this.props.value} id={this.props.id}
                  className="col-selector form-control">
            {this.props.cols.name.map(function (c) {
              return (<option value={c} key={c}>{c}</option>);
            })}
          </select>
        );
  }
});

/** UI for {stat: {name: "stat-name", opts: {opts}}}
 */
var StatSelector = React.createClass({
  propTypes: {
    stat: React.PropTypes.object.isRequired,
    schema: React.PropTypes.object.isRequired
  },
  handleChange: function (event) {
    var val = event.target.value;
    this.props.handleAesInput({stat: {name: {$set: val}}});
  },

  render: function () {
    return (
        <div>
          <select value={this.props.stat.name} onChange={this.handleChange}
                  id={this.props.id} className="form-control">
            {this.props.schema.name.map(function (stat) {
              return (
                  <option value={stat} key={stat}>{stat}</option>)
            })}
          </select>
        </div>);
  }
});


var StatOptsBin = React.createClass({
  handleChange: function() {
    var binOpts = {lower: this.refs.lower.getDOMNode().value,
      upper: this.refs.upper.getDOMNode().value,
      nbins: this.refs.nbins.getDOMNode().value};
    this.props.handleAesInput({stat: {opts: {$set: binOpts}}});
  },
  render: function() {
    return (
        <div className="row">
          <div className="col-xs-4" style={{'padding-right': '0px'}}>
            <label htmlFor={this.props.id+"-lower"}>start</label>
            <input id={this.props.id+"-lower"} className="form-control" type="number" ref="lower" value={this.props.lower}
                   onChange={this.handleChange} placeholder="start" />
          </div>
          <div className="col-xs-4" style={{'padding-left': '7px', 'padding-right': '8px'}}>
            <label htmlFor={this.props.id+"-upper"}>end</label>
            <input className="form-control" type="number" ref="upper" value={this.props.upper}
                   placeholder="start" onChange={this.handleChange} id={this.props.id+"-upper"}/>
          </div>
          <div className="col-xs-4" style={{'padding-left': '0px'}}>
            <label htmlFor={this.props.id+"-nbins"}>bins</label>
            <input className="form-control" type="number" ref="nbins" value={this.props.nbins}
                   placeholder="start" onChange={this.handleChange} id={this.props.id+"-nbins"} />
          </div>
        </div>);
  }
});

var StatOptsSelector = React.createClass({

  render: function() {
    var hiddenStyle = {display: "hidden"};
    if (this.props.stat === "bin") {
      return <StatOptsBin args={this.props.statOpts.bin} handleAesInput={this.props.handleAesInput} />
    }
    return (

        <div style={hiddenStyle}></div>
    );
  }
});

/** Radio button for selecting preset */
var AesPresetRadio = React.createClass({

  render: function () {
    return (
        <label className="radio-inline geom">
          <input type="radio" name="aes-preset" value={this.props.value}
                 onChange={this.handleChange} />
          <div className="row">
            <img src={this.props.thumbnail} width="50px" height="50px" />
          </div>
          <div className="row">{this.props.value}</div>
        </label>
    )
  }
});

var PlotResult = React.createClass({
  render: function () {
    return (
        <div id="plot-result">
          <div dangerouslySetInnerHTML={{__html: this.props.data}} />
        </div>);
  }
});

