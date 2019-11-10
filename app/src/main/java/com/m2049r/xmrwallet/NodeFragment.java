/*
 * Copyright (c) 2018 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.levin.scanner.Dispatcher;
import com.m2049r.xmrwallet.data.Node;
import com.m2049r.xmrwallet.data.NodeInfo;
import com.m2049r.xmrwallet.layout.NodeInfoAdapter;
import com.m2049r.xmrwallet.model.NetworkType;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.Notice;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import timber.log.Timber;

public class NodeFragment extends Fragment
        implements NodeInfoAdapter.OnInteractionListener, View.OnClickListener {

    static private int NODES_TO_FIND = 10;

    static private NumberFormat FORMATTER = NumberFormat.getInstance();

    private SwipeRefreshLayout pullToRefresh;
    private TextView tvPull;
    private View fab;

    private Set<NodeInfo> nodeList = new HashSet<>();

    private NodeInfoAdapter nodesAdapter;

    private Listener activityCallback;

    public interface Listener {
        File getStorageRoot();

        void setToolbarButton(int type);

        void setSubtitle(String title);

        Set<NodeInfo> getFavouriteNodes();

        void setFavouriteNodes(Set<NodeInfo> favouriteNodes);
    }

    void filterFavourites() {
        for (Iterator<NodeInfo> iter = nodeList.iterator(); iter.hasNext(); ) {
            Node node = iter.next();
            if (!node.isFavourite()) iter.remove();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            this.activityCallback = (Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    @Override
    public void onPause() {
        Timber.d("onPause() %d", nodeList.size());
        if (asyncFindNodes != null)
            asyncFindNodes.cancel(true);
        if (activityCallback != null)
            activityCallback.setFavouriteNodes(nodeList);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        activityCallback.setSubtitle(getString(R.string.label_nodes));
        updateRefreshElements();
    }

    boolean isRefreshing() {
        return asyncFindNodes != null;
    }

    void updateRefreshElements() {
        if (isRefreshing()) {
            activityCallback.setToolbarButton(Toolbar.BUTTON_NONE);
            fab.setVisibility(View.GONE);
        } else {
            activityCallback.setToolbarButton(Toolbar.BUTTON_BACK);
            fab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView");
        View view = inflater.inflate(R.layout.fragment_node, container, false);

        fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(this);

        RecyclerView recyclerView = view.findViewById(R.id.list);
        nodesAdapter = new NodeInfoAdapter(getActivity(), this);
        recyclerView.setAdapter(nodesAdapter);

        tvPull = view.findViewById(R.id.tvPull);

        pullToRefresh = view.findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (WalletManager.getInstance().getNetworkType() == NetworkType.NetworkType_Mainnet) {
                    refresh();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.node_wrong_net), Toast.LENGTH_LONG).show();
                    pullToRefresh.setRefreshing(false);
                }
            }
        });

        Helper.hideKeyboard(getActivity());

        nodeList = new HashSet<>(activityCallback.getFavouriteNodes());
        nodesAdapter.setNodes(nodeList);

        ViewGroup llNotice = view.findViewById(R.id.llNotice);
        Notice.showAll(llNotice, ".*_nodes");

        return view;
    }

    private AsyncFindNodes asyncFindNodes = null;

    private void refresh() {
        if (asyncFindNodes != null) return; // ignore refresh request as one is ongoing
        asyncFindNodes = new AsyncFindNodes();
        updateRefreshElements();
        asyncFindNodes.execute();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.node_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // Callbacks from NodeInfoAdapter
    @Override
    public void onInteraction(final View view, final NodeInfo nodeItem) {
        Timber.d("onInteraction");
        EditDialog diag = createEditDialog(nodeItem);
        if (diag != null) {
            diag.show();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.fab:
                EditDialog diag = createEditDialog(null);
                if (diag != null) {
                    diag.show();
                }
                break;
        }
    }

    private class AsyncFindNodes extends AsyncTask<Void, NodeInfo, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            filterFavourites();
            nodesAdapter.setNodes(null);
            nodesAdapter.allowClick(false);
            tvPull.setText(getString(R.string.node_scanning));
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Timber.d("scanning");
            Set<NodeInfo> seedList = new HashSet<>();
            seedList.addAll(nodeList);
            nodeList.clear();
            Timber.d("seed %d", seedList.size());
            Dispatcher d = new Dispatcher(new Dispatcher.Listener() {
                @Override
                public void onGet(NodeInfo info) {
                    publishProgress(info);
                }
            });
            d.seedPeers(seedList);
            d.awaitTermination(NODES_TO_FIND);

            // we didn't find enough because we didn't ask around enough? ask more!
            if ((d.getRpcNodes().size() < NODES_TO_FIND) &&
                    (d.getPeerCount() < NODES_TO_FIND + seedList.size())) {
                // try again
                publishProgress((NodeInfo[]) null);
                d = new Dispatcher(new Dispatcher.Listener() {
                    @Override
                    public void onGet(NodeInfo info) {
                        publishProgress(info);
                    }
                });
                // also seed with monero seed nodes (see p2p/net_node.inl:410 in monero src)
                seedList.add(new NodeInfo(new InetSocketAddress("107.152.130.98", 18080)));
                seedList.add(new NodeInfo(new InetSocketAddress("212.83.175.67", 18080)));
                seedList.add(new NodeInfo(new InetSocketAddress("5.9.100.248", 18080)));
                seedList.add(new NodeInfo(new InetSocketAddress("163.172.182.165", 18080)));
                seedList.add(new NodeInfo(new InetSocketAddress("161.67.132.39", 18080)));
                seedList.add(new NodeInfo(new InetSocketAddress("198.74.231.92", 18080)));
                seedList.add(new NodeInfo(new InetSocketAddress("195.154.123.123", 18080)));
                seedList.add(new NodeInfo(new InetSocketAddress("212.83.172.165", 18080)));
                seedList.add(new NodeInfo(new InetSocketAddress("192.110.160.146", 18080)));
                d.seedPeers(seedList);
                d.awaitTermination(NODES_TO_FIND);
            }
            // final (filtered) result
            nodeList.addAll(d.getRpcNodes());
            return true;
        }

        @Override
        protected void onProgressUpdate(NodeInfo... values) {
            Timber.d("onProgressUpdate");
            if (!isCancelled())
                if (values != null)
                    nodesAdapter.addNode(values[0]);
                else
                    nodesAdapter.setNodes(null);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Timber.d("done scanning");
            complete();
        }

        @Override
        protected void onCancelled(Boolean result) {
            Timber.d("cancelled scanning");
            complete();
        }

        private void complete() {
            asyncFindNodes = null;
            if (!isAdded()) return;
            //if (isCancelled()) return;
            tvPull.setText(getString(R.string.node_pull_hint));
            pullToRefresh.setRefreshing(false);
            nodesAdapter.setNodes(nodeList);
            nodesAdapter.allowClick(true);
            updateRefreshElements();
        }
    }

    @Override
    public void onDetach() {
        Timber.d("detached");
        super.onDetach();
    }

    private EditDialog editDialog = null; // for preventing opening of multiple dialogs

    private EditDialog createEditDialog(final NodeInfo nodeInfo) {
        if (editDialog != null) return null; // we are already open
        editDialog = new EditDialog(nodeInfo);
        return editDialog;
    }

    class EditDialog {
        final NodeInfo nodeInfo;
        final NodeInfo nodeBackup;

        private boolean applyChanges() {
            nodeInfo.clear();
            showTestResult();

            final String portString = etNodePort.getEditText().getText().toString().trim();
            int port;
            if (portString.isEmpty()) {
                port = Node.getDefaultRpcPort();
            } else {
                try {
                    port = Integer.parseInt(portString);
                } catch (NumberFormatException ex) {
                    etNodePort.setError(getString(R.string.node_port_numeric));
                    return false;
                }
            }
            etNodePort.setError(null);
            if ((port <= 0) || (port > 65535)) {
                etNodePort.setError(getString(R.string.node_port_range));
                return false;
            }

            final String host = etNodeHost.getEditText().getText().toString().trim();
            if (host.isEmpty()) {
                etNodeHost.setError(getString(R.string.node_host_empty));
                return false;
            }
            final boolean setHostSuccess = Helper.runWithNetwork(new Helper.Action() {
                @Override
                public boolean run() {
                    try {
                        nodeInfo.setHost(host);
                        return true;
                    } catch (UnknownHostException ex) {
                        etNodeHost.setError(getString(R.string.node_host_unresolved));
                        return false;
                    }
                }
            });
            if (!setHostSuccess) {
                etNodeHost.setError(getString(R.string.node_host_unresolved));
                return false;
            }
            etNodeHost.setError(null);
            nodeInfo.setRpcPort(port);
            // setName() may trigger reverse DNS
            Helper.runWithNetwork(new Helper.Action() {
                @Override
                public boolean run() {
                    nodeInfo.setName(etNodeName.getEditText().getText().toString().trim());
                    return true;
                }
            });
            nodeInfo.setUsername(etNodeUser.getEditText().getText().toString().trim());
            nodeInfo.setPassword(etNodePass.getEditText().getText().toString()); // no trim for pw
            return true;
        }

        private boolean shutdown = false;

        private void apply() {
            if (applyChanges()) {
                closeDialog();
                if (nodeBackup == null) { // this is a (FAB) new node
                    nodeInfo.setFavourite(true);
                    nodeList.add(nodeInfo);
                }
                shutdown = true;
                new AsyncTestNode().execute();
            }
        }

        private void closeDialog() {
            if (editDialog == null) throw new IllegalStateException();
            Helper.hideKeyboardAlways(getActivity());
            editDialog.dismiss();
            editDialog = null;
            NodeFragment.this.editDialog = null;
        }

        private void undoChanges() {
            if (nodeBackup != null)
                nodeInfo.overwriteWith(nodeBackup);
        }

        private void show() {
            editDialog.show();
        }

        private void test() {
            if (applyChanges())
                new AsyncTestNode().execute();
        }

        private void showKeyboard() {
            Helper.showKeyboard(editDialog);
        }

        AlertDialog editDialog = null;

        TextInputLayout etNodeName;
        TextInputLayout etNodeHost;
        TextInputLayout etNodePort;
        TextInputLayout etNodeUser;
        TextInputLayout etNodePass;
        TextView tvResult;

        void showTestResult() {
            if (nodeInfo.isSuccessful()) {
                tvResult.setText(getString(R.string.node_result,
                        FORMATTER.format(nodeInfo.getHeight()), nodeInfo.getMajorVersion(),
                        nodeInfo.getResponseTime(), nodeInfo.getHostAddress()));
            } else {
                tvResult.setText(NodeInfoAdapter.getResponseErrorText(getActivity(), nodeInfo.getResponseCode()));
            }
        }

        EditDialog(final NodeInfo nodeInfo) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            LayoutInflater li = LayoutInflater.from(alertDialogBuilder.getContext());
            View promptsView = li.inflate(R.layout.prompt_editnode, null);
            alertDialogBuilder.setView(promptsView);

            etNodeName = promptsView.findViewById(R.id.etNodeName);
            etNodeHost = promptsView.findViewById(R.id.etNodeHost);
            etNodePort = promptsView.findViewById(R.id.etNodePort);
            etNodeUser = promptsView.findViewById(R.id.etNodeUser);
            etNodePass = promptsView.findViewById(R.id.etNodePass);
            tvResult = promptsView.findViewById(R.id.tvResult);

            if (nodeInfo != null) {
                this.nodeInfo = nodeInfo;
                nodeBackup = new NodeInfo(nodeInfo);
                etNodeName.getEditText().setText(nodeInfo.getName());
                etNodeHost.getEditText().setText(nodeInfo.getHost());
                etNodePort.getEditText().setText(Integer.toString(nodeInfo.getRpcPort()));
                etNodeUser.getEditText().setText(nodeInfo.getUsername());
                etNodePass.getEditText().setText(nodeInfo.getPassword());
                showTestResult();
            } else {
                this.nodeInfo = new NodeInfo();
                nodeBackup = null;
            }

            // set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.label_ok), null)
                    .setNeutralButton(getString(R.string.label_test), null)
                    .setNegativeButton(getString(R.string.label_cancel),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    undoChanges();
                                    closeDialog();
                                    nodesAdapter.dataSetChanged(); // to refresh test results
                                }
                            });

            editDialog = alertDialogBuilder.create();
            // these need to be here, since we don't always close the dialog
            editDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(final DialogInterface dialog) {
                    Button testButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
                    testButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            test();
                        }
                    });

                    Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            apply();
                        }
                    });
                }
            });

            if (Helper.preventScreenshot()) {
                editDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            }

            etNodePass.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        editDialog.getButton(DialogInterface.BUTTON_NEUTRAL).requestFocus();
                        test();
                        return true;
                    }
                    return false;
                }
            });
        }

        private class AsyncTestNode extends AsyncTask<Void, Void, Boolean> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                nodeInfo.clear();
                tvResult.setText(getString(R.string.node_testing, nodeInfo.getHostAddress()));
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                nodeInfo.testRpcService();
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (editDialog != null) {
                    showTestResult();
                }
                if (shutdown) {
                    if (nodeBackup == null) {
                        nodesAdapter.addNode(nodeInfo);
                    } else {
                        nodesAdapter.dataSetChanged();
                    }
                }
            }
        }
    }
}